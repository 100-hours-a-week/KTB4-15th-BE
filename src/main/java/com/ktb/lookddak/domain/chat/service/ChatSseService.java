package com.ktb.lookddak.domain.chat.service;

import com.ktb.lookddak.domain.chat.dto.ChatSseConnectedResponse;
import com.ktb.lookddak.domain.chat.dto.ChatSseSnapshotResponse;
import com.ktb.lookddak.domain.chat.dto.ChatSseTokenResponse;
import com.ktb.lookddak.domain.chat.entity.ChatMessage;
import com.ktb.lookddak.domain.chat.entity.ChatSenderType;
import com.ktb.lookddak.domain.chat.entity.ChatRoom;
import com.ktb.lookddak.domain.chat.repository.ChatMessageRepository;
import com.ktb.lookddak.domain.chat.repository.ChatRoomRepository;
import com.ktb.lookddak.global.exception.BusinessException;
import com.ktb.lookddak.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
public class ChatSseService {

    private static final long SSE_TIMEOUT_MILLIS = 5 * 60 * 1000L;

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatSseSnapshotStore snapshotStore;
    private final ConcurrentMap<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

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

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        ChatSseSnapshotStore.SnapshotState state = snapshotStore
                .getOrCreate(userMessageId);
        synchronized (state.getLock()) {
            register(userMessageId, emitter);
            send(userMessageId, emitter, "connected",
                    new ChatSseConnectedResponse(userMessageId));

            String snapshot = state.content();
            if (!snapshot.isBlank()) {
                send(userMessageId, emitter, "snapshot",
                        new ChatSseSnapshotResponse(userMessageId, snapshot));
            }
        }

        return emitter;
    }

    private void validateMessageAccess(
            Long memberId,
            Long chatRoomId,
            Long userMessageId
    ) {
        ChatRoom chatRoom = chatRoomRepository.findActiveById(chatRoomId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.CHAT_ROOM_NOT_FOUND
                ));

        if (!chatRoom.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }

        ChatMessage message = chatMessageRepository
                .findByIdAndChatRoomId(userMessageId, chatRoomId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.CHAT_MESSAGE_NOT_FOUND
                ));

        if (message.getSenderType() != ChatSenderType.USER) {
            throw new BusinessException(ErrorCode.CHAT_MESSAGE_NOT_FOUND);
        }
    }

    private void register(Long userMessageId, SseEmitter emitter) {
        SseEmitter previousEmitter = emitters.put(userMessageId, emitter);
        if (previousEmitter != null) {
            previousEmitter.complete();
        }

        emitter.onCompletion(() -> emitters.remove(userMessageId, emitter));
        emitter.onTimeout(() -> emitters.remove(userMessageId, emitter));
        emitter.onError(exception -> emitters.remove(userMessageId, emitter));
    }

    /**
     * AI token을 누적한 뒤 현재 연결에 전달한다.
     * 재연결의 connected/snapshot 전송과 같은 메시지 단위 락을 사용해
     * 새 연결에서 snapshot 다음에만 token이 전달되도록 보장한다.
     */
    public void sendToken(Long userMessageId, String content) {
        ChatSseSnapshotStore.SnapshotState state = snapshotStore
                .getOrCreate(userMessageId);
        synchronized (state.getLock()) {
            state.append(content);
            sendToCurrentEmitter(userMessageId, "token",
                    new ChatSseTokenResponse(userMessageId, content));
        }
    }

    private void send(
            Long userMessageId,
            SseEmitter emitter,
            String eventName,
            Object data
    ) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
        } catch (IOException exception) {
            emitters.remove(userMessageId, emitter);
            emitter.completeWithError(exception);
        }
    }

    /**
     * 현재 연결된 프런트에 AI 이벤트를 전달한다.
     * 연결이 이미 끊긴 경우 AI 작업은 중단하지 않고 이벤트만 전달하지 않는다.
     */
    public void send(Long userMessageId, String eventName, Object data) {
        ChatSseSnapshotStore.SnapshotState state = snapshotStore
                .getOrCreate(userMessageId);
        synchronized (state.getLock()) {
            sendToCurrentEmitter(userMessageId, eventName, data);
        }
    }

    private void sendToCurrentEmitter(
            Long userMessageId,
            String eventName,
            Object data
    ) {
        SseEmitter emitter = emitters.get(userMessageId);
        if (emitter != null) {
            send(userMessageId, emitter, eventName, data);
        }
    }

    public void complete(Long userMessageId) {
        ChatSseSnapshotStore.SnapshotState state = snapshotStore
                .getOrCreate(userMessageId);
        synchronized (state.getLock()) {
            SseEmitter emitter = emitters.remove(userMessageId);
            if (emitter != null) {
                emitter.complete();
            }
            snapshotStore.remove(userMessageId, state);
        }
    }

    boolean hasConnection(Long userMessageId) {
        return emitters.containsKey(userMessageId);
    }
}
