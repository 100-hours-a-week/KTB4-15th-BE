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
import org.springframework.stereotype.Service;
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
            removeConnection(userMessageId, emitter);
            emitter.completeWithError(exception);
            throw exception;
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
        if (previousEmitter != null) {
            previousEmitter.complete();
        }
        registerCallbacks(userMessageId, emitter, null);
        if (previousConnection != null) {
            synchronized (previousConnection.lock) {
                previousConnection.buffer.clear();
            }
        }
    }

    private void registerRedis(Long userMessageId, RedisConnection connection) {
        SseEmitter previousEmitter = emitters.put(userMessageId, connection.emitter);
        RedisConnection previousConnection = redisConnections.put(userMessageId, connection);
        if (previousEmitter != null) {
            previousEmitter.complete();
        }
        if (previousConnection != null) {
            synchronized (previousConnection.lock) {
                previousConnection.buffer.clear();
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
            completeRedisConnection(userMessageId, connection);
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
        if (emitter != null) {
            emitter.complete();
        }
        if (connection != null) {
            synchronized (connection.lock) {
                connection.buffer.clear();
            }
        }
        ChatSseSnapshotStore.SnapshotState state = snapshotStore.getOrCreate(userMessageId);
        snapshotStore.remove(userMessageId, state);
    }

    private void completeRedisConnection(Long userMessageId, RedisConnection connection) {
        if (emitters.remove(userMessageId, connection.emitter)) {
            connection.emitter.complete();
        }
        redisConnections.remove(userMessageId, connection);
    }

    private void removeConnection(Long userMessageId, SseEmitter emitter) {
        emitters.remove(userMessageId, emitter);
        redisConnections.computeIfPresent(userMessageId, (ignored, connection) ->
                connection.emitter == emitter ? null : connection
        );
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
