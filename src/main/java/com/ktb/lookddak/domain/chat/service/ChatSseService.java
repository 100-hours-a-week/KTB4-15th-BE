package com.ktb.lookddak.domain.chat.service;

import com.ktb.lookddak.domain.chat.dto.ChatSseConnectedResponse;
import com.ktb.lookddak.domain.chat.dto.ChatSseDoneResponse;
import com.ktb.lookddak.domain.chat.dto.ChatSseErrorResponse;
import com.ktb.lookddak.domain.chat.dto.ChatSseSnapshotResponse;
import com.ktb.lookddak.domain.chat.dto.ChatSseTokenResponse;
import com.ktb.lookddak.domain.chat.dto.ChatGenerationStatusResponse;
import com.ktb.lookddak.domain.chat.dto.ChatMessageDetailResponse;
import com.ktb.lookddak.domain.chat.dto.RecommendedProductResponse;
import com.ktb.lookddak.domain.chat.entity.ChatGenerationStatus;
import com.ktb.lookddak.domain.chat.entity.ChatMessage;
import com.ktb.lookddak.domain.chat.entity.ChatRoom;
import com.ktb.lookddak.domain.chat.entity.ChatSenderType;
import com.ktb.lookddak.domain.chat.redis.ChatRedisEvent;
import com.ktb.lookddak.domain.chat.redis.ChatRedisEventType;
import com.ktb.lookddak.domain.chat.redis.ChatRedisProperties;
import com.ktb.lookddak.domain.chat.redis.ChatRedisSnapshot;
import com.ktb.lookddak.domain.chat.redis.ChatRedisSnapshotRepository;
import com.ktb.lookddak.domain.chat.repository.ChatMessageRepository;
import com.ktb.lookddak.domain.chat.repository.ChatRoomRepository;
import com.ktb.lookddak.global.exception.BusinessException;
import com.ktb.lookddak.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatSseService {

    private static final long SSE_TIMEOUT_MILLIS = 5 * 60 * 1000L;
    private static final String RECOVERY_OVERFLOW_CODE = "CHAT_STREAM_RECOVERY_OVERFLOW";

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatSseSnapshotStore snapshotStore;
    private final ChatService chatService;
    private final ChatRedisProperties chatRedisProperties;
    private final ChatRedisSnapshotRepository chatRedisSnapshotRepository;
    private final ConcurrentMap<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, RedisConnection> redisConnections = new ConcurrentHashMap<>();
    /** Redis가 내려간 동안에는 Pub/Sub 대신 DB terminal 상태만 확인하는 SSE 연결이다. */
    private final ConcurrentMap<Long, RedisConnection> degradedRedisConnections = new ConcurrentHashMap<>();

    /**
     * SSE 연결을 생성하고, 현재 로그인한 회원이 요청한 메시지를 조회할 권한이 있는지 확인한다.
     * 연결을 보관하는 동안에는 DB 트랜잭션이나 DB 커넥션을 유지하지 않는다.
     */
    public SseEmitter connect(
            Long memberId,
            Long chatRoomId,
            Long userMessageId
    ) {
        validateMessageAccess(memberId, chatRoomId, userMessageId);
        if (chatRedisProperties.isEnabled()) {
            ChatGenerationStatusResponse generation = chatService.getGenerationStatus(
                    memberId,
                    chatRoomId,
                    userMessageId
            );
            if (isTerminal(generation.getGenerationStatus())) {
                return connectWithTerminalDatabaseResult(userMessageId, generation);
            }
            return connectWithRedisSnapshot(memberId, chatRoomId, userMessageId);
        }
        return connectWithLocalSnapshot(userMessageId);
    }

    private SseEmitter connectWithLocalSnapshot(Long userMessageId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        ChatSseSnapshotStore.SnapshotState state = snapshotStore.getOrCreate(userMessageId);
        synchronized (state.getLock()) {
            registerLocal(userMessageId, emitter);
            send(userMessageId, emitter, "connected", new ChatSseConnectedResponse(userMessageId));

            String snapshot = state.content();
            if (!snapshot.isBlank()) {
                send(userMessageId, emitter, "snapshot",
                        new ChatSseSnapshotResponse(userMessageId, snapshot));
            }
        }
        return emitter;
    }

    /**
     * 먼저 RECOVERING 연결을 등록한 뒤 Snapshot을 읽는다. 이 사이 도착한 Pub/Sub event는
     * connection 버퍼에 쌓고, snapshot의 seq보다 큰 event만 snapshot 뒤에 이어 보낸다.
     */
    private SseEmitter connectWithRedisSnapshot(
            Long memberId,
            Long chatRoomId,
            Long userMessageId
    ) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        RedisConnection connection = new RedisConnection(emitter, memberId, chatRoomId);
        registerRedis(userMessageId, connection);
        synchronized (connection.lock) {
            send(userMessageId, emitter, "connected", new ChatSseConnectedResponse(userMessageId));
        }

        Optional<ChatRedisSnapshot> snapshot;
        try {
            snapshot = chatRedisSnapshotRepository.findByMessageId(userMessageId);
        } catch (RuntimeException exception) {
            // Redis 장애 중에도 같은 JVM의 local Snapshot은 전송할 수 있다. 동시에 이 연결을
            // degraded 목록에 보관해, 생성 BE가 DB에 terminal 결과를 저장하면 기존 SSE로 보낸다.
            switchRedisConnectionToLocal(userMessageId, connection);
            sendLocalSnapshot(userMessageId, emitter);
            return emitter;
        }
        synchronized (connection.lock) {
            if (redisConnections.get(userMessageId) != connection) {
                return emitter;
            }

            if (connection.bufferOverflowed) {
                sendRecoveryOverflow(userMessageId, connection);
                return emitter;
            }

            connection.lastSeq = snapshot.map(ChatRedisSnapshot::seq).orElse(0L);
            snapshot.map(ChatRedisSnapshot::content)
                    .filter(content -> !content.isBlank())
                    .ifPresent(content -> send(
                            userMessageId,
                            emitter,
                            "snapshot",
                            new ChatSseSnapshotResponse(userMessageId, content)
                    ));

            connection.buffer.sort(Comparator.comparingLong(ChatRedisEvent::seq));
            List<ChatRedisEvent> bufferedEvents = new ArrayList<>(connection.buffer);
            connection.buffer.clear();
            for (ChatRedisEvent bufferedEvent : bufferedEvents) {
                forwardRedisEventLocked(userMessageId, connection, bufferedEvent);
            }
            connection.recovering = false;
        }
        snapshot.filter(value -> isTerminal(value.status()))
                .ifPresent(ignored -> sendTerminalResultFromDatabase(
                        memberId,
                        chatRoomId,
                        userMessageId,
                        connection
                ));
        return emitter;
    }

    /**
     * 완료된 요청은 Redis 보관 기간과 무관하게 DB를 진실 공급원으로 사용한다.
     * 따라서 마지막 Pub/Sub done/error를 놓쳤거나 Snapshot이 만료돼도 복구할 수 있다.
     */
    private SseEmitter connectWithTerminalDatabaseResult(
            Long userMessageId,
            ChatGenerationStatusResponse result
    ) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        registerLocal(userMessageId, emitter);
        send(userMessageId, emitter, "connected", new ChatSseConnectedResponse(userMessageId));

        if (result.getGenerationStatus() == ChatGenerationStatus.COMPLETED
                && result.getMessage() != null) {
            ChatMessageDetailResponse message = result.getMessage();
            List<RecommendedProductResponse> products = message.getRecommendation() == null
                    ? List.of()
                    : message.getRecommendation().getProducts();
            send(userMessageId, emitter, "done", new ChatSseDoneResponse(
                    userMessageId,
                    message.getContent(),
                    products
            ));
        } else {
            send(userMessageId, emitter, "error", new ChatSseErrorResponse(
                    userMessageId,
                    "AI_GENERATION_FAILED",
                    "AI 응답 생성에 실패했습니다. 다시 시도해 주세요."
            ));
        }
        complete(userMessageId);
        return emitter;
    }

    private void sendTerminalResultFromDatabase(
            Long memberId,
            Long chatRoomId,
            Long userMessageId,
            RedisConnection connection
    ) {
        if (redisConnections.get(userMessageId) != connection) {
            return;
        }
        ChatGenerationStatusResponse result = chatService.getGenerationStatus(
                memberId,
                chatRoomId,
                userMessageId
        );
        synchronized (connection.lock) {
            if (redisConnections.get(userMessageId) != connection) {
                return;
            }
            if (result.getGenerationStatus() == ChatGenerationStatus.COMPLETED
                    && result.getMessage() != null) {
                ChatMessageDetailResponse message = result.getMessage();
                List<RecommendedProductResponse> products = message.getRecommendation() == null
                        ? List.of()
                        : message.getRecommendation().getProducts();
                send(userMessageId, connection.emitter, "done", new ChatSseDoneResponse(
                        userMessageId,
                        message.getContent(),
                        products
                ));
                completeRedisConnection(userMessageId, connection);
                return;
            }
            if (result.getGenerationStatus() == ChatGenerationStatus.FAILED) {
                send(userMessageId, connection.emitter, "error", new ChatSseErrorResponse(
                        userMessageId,
                        "AI_GENERATION_FAILED",
                        "AI 응답 생성에 실패했습니다. 다시 시도해 주세요."
                ));
                completeRedisConnection(userMessageId, connection);
            }
        }
    }

    private boolean isTerminal(ChatGenerationStatus status) {
        return status == ChatGenerationStatus.COMPLETED || status == ChatGenerationStatus.FAILED;
    }

    private void validateMessageAccess(
            Long memberId,
            Long chatRoomId,
            Long userMessageId
    ) {
        ChatRoom chatRoom = chatRoomRepository.findActiveById(chatRoomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        if (!chatRoom.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }

        ChatMessage message = chatMessageRepository.findByIdAndChatRoomId(userMessageId, chatRoomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_MESSAGE_NOT_FOUND));
        if (message.getSenderType() != ChatSenderType.USER) {
            throw new BusinessException(ErrorCode.CHAT_MESSAGE_NOT_FOUND);
        }
    }

    private void registerLocal(Long userMessageId, SseEmitter emitter) {
        SseEmitter previousEmitter = emitters.put(userMessageId, emitter);
        RedisConnection previousConnection = redisConnections.remove(userMessageId);
        RedisConnection previousDegradedConnection = degradedRedisConnections.remove(userMessageId);
        if (previousEmitter != null) {
            previousEmitter.complete();
        }
        registerCallbacks(userMessageId, emitter, null);
        if (previousConnection != null) {
            synchronized (previousConnection.lock) {
                previousConnection.buffer.clear();
            }
        }
        if (previousDegradedConnection != null) {
            synchronized (previousDegradedConnection.lock) {
                previousDegradedConnection.buffer.clear();
            }
        }
    }

    private void registerRedis(Long userMessageId, RedisConnection connection) {
        SseEmitter previousEmitter = emitters.put(userMessageId, connection.emitter);
        RedisConnection previousConnection = redisConnections.put(userMessageId, connection);
        RedisConnection previousDegradedConnection = degradedRedisConnections.remove(userMessageId);
        if (previousEmitter != null) {
            previousEmitter.complete();
        }
        if (previousConnection != null) {
            synchronized (previousConnection.lock) {
                previousConnection.buffer.clear();
            }
        }
        if (previousDegradedConnection != null) {
            synchronized (previousDegradedConnection.lock) {
                previousDegradedConnection.buffer.clear();
            }
        }
        registerCallbacks(userMessageId, connection.emitter, connection);
    }

    private void registerCallbacks(
            Long userMessageId,
            SseEmitter emitter,
            RedisConnection connection
    ) {
        Runnable cleanup = () -> {
            emitters.remove(userMessageId, emitter);
            if (connection != null) {
                redisConnections.remove(userMessageId, connection);
                degradedRedisConnections.remove(userMessageId, connection);
            }
        };
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(exception -> cleanup.run());
    }

    /** 기존 Redis 비활성 경로에서만 사용하는 로컬 Snapshot 누적 전송이다. */
    public void sendToken(Long userMessageId, String content) {
        ChatSseSnapshotStore.SnapshotState state = snapshotStore.getOrCreate(userMessageId);
        synchronized (state.getLock()) {
            state.append(content);
            sendToCurrentEmitter(userMessageId, "token", new ChatSseTokenResponse(userMessageId, content));
        }
    }

    /** Redis 장애 fallback을 위해 생성 BE에 누적 본문만 유지한다. */
    public void appendLocalSnapshot(Long userMessageId, String content) {
        ChatSseSnapshotStore.SnapshotState state = snapshotStore.getOrCreate(userMessageId);
        synchronized (state.getLock()) {
            state.append(content);
        }
    }

    /** 이미 local Snapshot에 기록한 token을 현재 JVM의 SSE 연결에만 직접 전달한다. */
    public void sendLocalToken(Long userMessageId, String content) {
        sendToCurrentEmitter(userMessageId, "token", new ChatSseTokenResponse(userMessageId, content));
    }

    /** Redis Pub/Sub subscriber가 호출하는 다중 인스턴스 이벤트 전달 진입점이다. */
    public void handleRedisEvent(ChatRedisEvent event) {
        RedisConnection connection = redisConnections.get(event.userMessageId());
        if (connection == null) {
            return;
        }
        boolean shouldResynchronize = false;
        synchronized (connection.lock) {
            if (redisConnections.get(event.userMessageId()) != connection) {
                return;
            }
            if (connection.recovering) {
                bufferRedisEvent(connection, event);
                return;
            }
            if (event.seq() > connection.lastSeq + 1) {
                // Pub/Sub은 보관하지 않으므로 seq gap은 누락 가능성으로 간주한다.
                // 먼저 event를 버퍼에 넣고 Snapshot을 다시 읽어 정합성을 맞춘다.
                connection.recovering = true;
                bufferRedisEvent(connection, event);
                shouldResynchronize = true;
            } else {
                forwardRedisEventLocked(event.userMessageId(), connection, event);
            }
        }
        if (shouldResynchronize) {
            resynchronizeRedisConnection(event.userMessageId(), connection);
        }
    }

    /**
     * Redis Pub/Sub listener가 연결 오류 뒤 재구독할 때 호출한다.
     * Pub/Sub은 단절 구간의 event를 재전달하지 않으므로 모든 활성 연결을 Snapshot으로
     * 재동기화해 누락을 보정한다.
     */
    public void resynchronizeAllRedisConnections() {
        redisConnections.forEach((userMessageId, connection) -> {
            boolean shouldResynchronize = false;
            synchronized (connection.lock) {
                if (redisConnections.get(userMessageId) != connection
                        || connection.recovering) {
                    return;
                }
                connection.recovering = true;
                shouldResynchronize = true;
            }
            if (shouldResynchronize) {
                resynchronizeRedisConnection(userMessageId, connection);
            }
        });
    }

    private void resynchronizeRedisConnection(Long userMessageId, RedisConnection connection) {
        Optional<ChatRedisSnapshot> snapshot;
        try {
            snapshot = chatRedisSnapshotRepository.findByMessageId(userMessageId);
        } catch (RuntimeException exception) {
            // Redis가 잠시 내려가도 현재 SSE를 닫지 않는다. 생성 BE는 local fallback으로
            // token을 계속 보낼 수 있고, 다른 BE 연결은 완료 뒤 DB 상태 조회로 복구한다.
            switchRedisConnectionToLocal(userMessageId, connection);
            return;
        }

        boolean needsAnotherResynchronization = false;
        synchronized (connection.lock) {
            if (redisConnections.get(userMessageId) != connection) {
                return;
            }
            if (connection.bufferOverflowed) {
                sendRecoveryOverflow(userMessageId, connection);
                return;
            }
            connection.lastSeq = snapshot.map(ChatRedisSnapshot::seq).orElse(connection.lastSeq);
            snapshot.map(ChatRedisSnapshot::content)
                    .filter(content -> !content.isBlank())
                    .ifPresent(content -> send(
                            userMessageId,
                            connection.emitter,
                            "snapshot",
                            new ChatSseSnapshotResponse(userMessageId, content)
                    ));

            connection.buffer.sort(Comparator.comparingLong(ChatRedisEvent::seq));
            List<ChatRedisEvent> bufferedEvents = new ArrayList<>(connection.buffer);
            connection.buffer.clear();
            for (int index = 0; index < bufferedEvents.size(); index++) {
                ChatRedisEvent bufferedEvent = bufferedEvents.get(index);
                if (bufferedEvent.seq() > connection.lastSeq + 1) {
                    // Snapshot 이후 버퍼 자체에 gap이 있으면 이 회차의 재생을 멈춘다.
                    // 아직 보내지 않은 event는 다음 Snapshot 재조회 뒤 다시 판정한다.
                    connection.buffer.addAll(bufferedEvents.subList(index, bufferedEvents.size()));
                    needsAnotherResynchronization = true;
                    break;
                }
                forwardRedisEventLocked(userMessageId, connection, bufferedEvent);
            }
            connection.recovering = needsAnotherResynchronization;
        }
        if (needsAnotherResynchronization) {
            resynchronizeRedisConnection(userMessageId, connection);
            return;
        }
        if (snapshot.map(ChatRedisSnapshot::status).filter(this::isTerminal).isPresent()) {
            sendTerminalResultFromDatabase(
                    connection.memberId,
                    connection.chatRoomId,
                    userMessageId,
                    connection
            );
        }
    }

    private void forwardRedisEventLocked(
            Long userMessageId,
            RedisConnection connection,
            ChatRedisEvent event
    ) {
        if (event.seq() <= connection.lastSeq) {
            return;
        }
        connection.lastSeq = event.seq();

        if (event.type() == ChatRedisEventType.TOKEN) {
            send(userMessageId, connection.emitter, "token",
                    new ChatSseTokenResponse(userMessageId, event.content()));
            return;
        }
        if (event.type() == ChatRedisEventType.DONE) {
            send(userMessageId, connection.emitter, "done", new ChatSseDoneResponse(
                    userMessageId, event.content(), event.products()
            ));
            completeRedisConnection(userMessageId, connection);
            return;
        }
        if (event.type() == ChatRedisEventType.ERROR) {
            send(userMessageId, connection.emitter, "error", new ChatSseErrorResponse(
                    userMessageId, event.code(), event.message()
            ));
            completeRedisConnection(userMessageId, connection);
        }
    }

    private void bufferRedisEvent(RedisConnection connection, ChatRedisEvent event) {
        if (connection.buffer.size() >= chatRedisProperties.getRecoveryBufferMaxEvents()) {
            connection.bufferOverflowed = true;
            return;
        }
        connection.buffer.add(event);
    }

    private void sendRecoveryOverflow(Long userMessageId, RedisConnection connection) {
        send(userMessageId, connection.emitter, "error", new ChatSseErrorResponse(
                userMessageId,
                RECOVERY_OVERFLOW_CODE,
                "응답 복구 버퍼가 가득 찼습니다. 다시 연결해 주세요."
        ));
        completeRedisConnection(userMessageId, connection);
    }

    private void send(Long userMessageId, SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException exception) {
            removeConnection(userMessageId, emitter);
            emitter.completeWithError(exception);
        }
    }

    /** 현재 연결된 프런트에 AI 이벤트를 전달한다. Redis 비활성 legacy 경로가 사용한다. */
    public void send(Long userMessageId, String eventName, Object data) {
        ChatSseSnapshotStore.SnapshotState state = snapshotStore.getOrCreate(userMessageId);
        synchronized (state.getLock()) {
            sendToCurrentEmitter(userMessageId, eventName, data);
        }
    }

    private void sendToCurrentEmitter(Long userMessageId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(userMessageId);
        if (emitter != null) {
            send(userMessageId, emitter, eventName, data);
        }
    }

    public void complete(Long userMessageId) {
        SseEmitter emitter = emitters.remove(userMessageId);
        RedisConnection connection = redisConnections.remove(userMessageId);
        RedisConnection degradedConnection = degradedRedisConnections.remove(userMessageId);
        if (emitter != null) {
            emitter.complete();
        }
        if (connection != null) {
            synchronized (connection.lock) {
                connection.buffer.clear();
            }
        }
        if (degradedConnection != null && degradedConnection != connection) {
            synchronized (degradedConnection.lock) {
                degradedConnection.buffer.clear();
            }
        }
        ChatSseSnapshotStore.SnapshotState state = snapshotStore.getOrCreate(userMessageId);
        snapshotStore.remove(userMessageId, state);
    }

    /**
     * Redis 자체를 사용할 수 없는 비상 경로다. Pub/Sub을 거치지 않고 현재 JVM에 연결된
     * 클라이언트에 terminal event를 보낸 뒤 정리한다. 다른 인스턴스의 연결은 DB 상태를
     * 조회하는 다음 재연결에서 terminal 결과를 받는다.
     */
    public void sendDirectTerminalAndComplete(Long userMessageId, String eventName, Object data) {
        send(userMessageId, eventName, data);
        complete(userMessageId);
    }

    private void completeRedisConnection(Long userMessageId, RedisConnection connection) {
        if (emitters.remove(userMessageId, connection.emitter)) {
            connection.emitter.complete();
        }
        redisConnections.remove(userMessageId, connection);
        degradedRedisConnections.remove(userMessageId, connection);
    }

    private void switchRedisConnectionToLocal(Long userMessageId, RedisConnection connection) {
        synchronized (connection.lock) {
            if (redisConnections.remove(userMessageId, connection)) {
                connection.buffer.clear();
                degradedRedisConnections.put(userMessageId, connection);
            }
        }
    }

    private void removeConnection(Long userMessageId, SseEmitter emitter) {
        emitters.remove(userMessageId, emitter);
        redisConnections.computeIfPresent(userMessageId, (ignored, connection) ->
                connection.emitter == emitter ? null : connection
        );
        degradedRedisConnections.computeIfPresent(userMessageId, (ignored, connection) ->
                connection.emitter == emitter ? null : connection
        );
    }

    /**
     * Redis 장애로 Pub/Sub을 받을 수 없게 된 기존 SSE 연결만 DB terminal 상태를 확인한다.
     * 프런트는 계속 같은 SSE를 기다리며, 완료되면 기존 done/error 계약으로 결과를 받는다.
     */
    @Scheduled(fixedDelayString = "${chat.redis.degraded-terminal-poll-interval:1s}")
    void recoverDegradedConnectionsFromDatabase() {
        detectRedisOutageForActiveConnections();
        degradedRedisConnections.forEach((userMessageId, connection) -> {
            if (emitters.get(userMessageId) != connection.emitter) {
                degradedRedisConnections.remove(userMessageId, connection);
                return;
            }
            try {
                ChatGenerationStatusResponse result = chatService.getGenerationStatus(
                        connection.memberId,
                        connection.chatRoomId,
                        userMessageId
                );
                if (isTerminal(result.getGenerationStatus())) {
                    sendTerminalResultFromDatabaseResult(userMessageId, connection, result);
                }
            } catch (RuntimeException exception) {
                // DB도 일시적으로 확인할 수 없으면 SSE를 끊지 않고 다음 주기에 다시 확인한다.
                log.warn("Failed to poll terminal chat status for degraded SSE userMessageId={}",
                        userMessageId, exception);
            }
        });
    }

    private void detectRedisOutageForActiveConnections() {
        if (redisConnections.isEmpty() || chatRedisSnapshotRepository.isAvailable()) {
            return;
        }
        // Pub/Sub listener가 재연결을 시도하면서 error callback을 늦게 호출하더라도,
        // 활성 SSE는 PING 실패를 기준으로 즉시 local/DB fallback 경로로 전환한다.
        redisConnections.forEach(this::switchRedisConnectionToLocal);
    }

    private void sendTerminalResultFromDatabaseResult(
            Long userMessageId,
            RedisConnection connection,
            ChatGenerationStatusResponse result
    ) {
        synchronized (connection.lock) {
            if (degradedRedisConnections.get(userMessageId) != connection) {
                return;
            }
            if (result.getGenerationStatus() == ChatGenerationStatus.COMPLETED
                    && result.getMessage() != null) {
                ChatMessageDetailResponse message = result.getMessage();
                List<RecommendedProductResponse> products = message.getRecommendation() == null
                        ? List.of()
                        : message.getRecommendation().getProducts();
                send(userMessageId, connection.emitter, "done", new ChatSseDoneResponse(
                        userMessageId, message.getContent(), products
                ));
                completeRedisConnection(userMessageId, connection);
                return;
            }
            if (result.getGenerationStatus() == ChatGenerationStatus.FAILED) {
                send(userMessageId, connection.emitter, "error", new ChatSseErrorResponse(
                        userMessageId,
                        "AI_GENERATION_FAILED",
                        "AI 응답 생성에 실패했습니다. 다시 시도해 주세요."
                ));
                completeRedisConnection(userMessageId, connection);
            }
        }
    }

    private void sendLocalSnapshot(Long userMessageId, SseEmitter emitter) {
        ChatSseSnapshotStore.SnapshotState state = snapshotStore.getOrCreate(userMessageId);
        synchronized (state.getLock()) {
            String snapshot = state.content();
            if (!snapshot.isBlank()) {
                send(userMessageId, emitter, "snapshot",
                        new ChatSseSnapshotResponse(userMessageId, snapshot));
            }
        }
    }

    boolean hasConnection(Long userMessageId) {
        return emitters.containsKey(userMessageId);
    }

    private static class RedisConnection {
        private final Object lock = new Object();
        private final SseEmitter emitter;
        private final Long memberId;
        private final Long chatRoomId;
        private final List<ChatRedisEvent> buffer = new ArrayList<>();
        private boolean recovering = true;
        private boolean bufferOverflowed;
        private long lastSeq;

        private RedisConnection(SseEmitter emitter, Long memberId, Long chatRoomId) {
            this.emitter = emitter;
            this.memberId = memberId;
            this.chatRoomId = chatRoomId;
        }
    }
}
