package com.ktb.lookddak.domain.chat.service;

import com.ktb.lookddak.domain.chat.dto.ChatSseConnectedResponse;
import com.ktb.lookddak.domain.chat.dto.ChatSseDoneResponse;
import com.ktb.lookddak.domain.chat.dto.ChatSseErrorResponse;
import com.ktb.lookddak.domain.chat.dto.ChatSseSnapshotResponse;
import com.ktb.lookddak.domain.chat.dto.ChatSseTokenResponse;
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

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatSseSnapshotStore snapshotStore;
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
            return connectWithRedisSnapshot(userMessageId);
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
    private SseEmitter connectWithRedisSnapshot(Long userMessageId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        RedisConnection connection = new RedisConnection(emitter);
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
        return emitter;
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
        synchronized (connection.lock) {
            if (redisConnections.get(event.userMessageId()) != connection) {
                return;
            }
            if (connection.recovering) {
                connection.buffer.add(event);
                return;
            }
            forwardRedisEventLocked(event.userMessageId(), connection, event);
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
        private final List<ChatRedisEvent> buffer = new ArrayList<>();
        private boolean recovering = true;
        private long lastSeq;

        private RedisConnection(SseEmitter emitter) {
            this.emitter = emitter;
        }
    }
}
