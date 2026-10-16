package com.ktb.lookddak.domain.notification.service;

import com.ktb.lookddak.domain.notification.dto.NotificationSseEventResponse;
import com.ktb.lookddak.domain.notification.dto.NotificationSseResponse;
import com.ktb.lookddak.domain.notification.entity.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSseService {

    private static final long SSE_TIMEOUT_MILLIS = 30 * 60 * 1000L;

    private final NotificationService notificationService;
    private final ConcurrentMap<Long, ConcurrentMap<String, SseEmitter>> emittersByMember =
            new ConcurrentHashMap<>();

    /**
     * 한 회원의 여러 브라우저·기기 연결을 별도로 보관한다.
     * SSE 연결을 보관하는 동안 DB 트랜잭션이나 커넥션은 유지하지 않는다.
     */
    public SseEmitter connect(Long memberId) {
        long unreadCount = notificationService.getUnreadCount(memberId);
        String connectionId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);

        register(memberId, connectionId, emitter);
        sendConnectedEvent(memberId, connectionId, emitter, unreadCount);
        return emitter;
    }

    public void sendNotification(
            Long memberId,
            Long notificationId,
            Long fittingJobId,
            NotificationType type
    ) {
        ConcurrentMap<String, SseEmitter> memberEmitters = emittersByMember.get(memberId);
        if (memberEmitters == null || memberEmitters.isEmpty()) {
            return;
        }

        NotificationSseEventResponse<NotificationSseResponse> response =
                NotificationSseEventResponse.received(new NotificationSseResponse(
                        notificationId,
                        fittingJobId,
                        type
                ));

        memberEmitters.forEach((connectionId, emitter) -> send(
                memberId,
                connectionId,
                emitter,
                "notification",
                response
        ));
    }

    private void register(
            Long memberId,
            String connectionId,
            SseEmitter emitter
    ) {
        emittersByMember.computeIfAbsent(memberId, ignored -> new ConcurrentHashMap<>())
                .put(connectionId, emitter);

        emitter.onCompletion(() -> remove(memberId, connectionId, emitter));
        emitter.onTimeout(() -> remove(memberId, connectionId, emitter));
        emitter.onError(exception -> remove(memberId, connectionId, emitter));
    }

    private void sendConnectedEvent(
            Long memberId,
            String connectionId,
            SseEmitter emitter,
            long unreadCount
    ) {
        send(
                memberId,
                connectionId,
                emitter,
                "connected",
                NotificationSseEventResponse.connected(unreadCount)
        );
    }

    private void send(
            Long memberId,
            String connectionId,
            SseEmitter emitter,
            String eventName,
            Object data
    ) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException exception) {
            log.info(
                    "NOTIFICATION_SSE_SEND_FAILED memberId={} connectionId={} event={}",
                    memberId,
                    connectionId,
                    eventName,
                    exception
            );
            remove(memberId, connectionId, emitter);
            emitter.completeWithError(exception);
        }
    }

    private void remove(
            Long memberId,
            String connectionId,
            SseEmitter emitter
    ) {
        emittersByMember.computeIfPresent(memberId, (ignored, memberEmitters) -> {
            memberEmitters.remove(connectionId, emitter);
            return memberEmitters.isEmpty() ? null : memberEmitters;
        });
    }

    boolean hasConnection(Long memberId) {
        ConcurrentMap<String, SseEmitter> memberEmitters = emittersByMember.get(memberId);
        return memberEmitters != null && !memberEmitters.isEmpty();
    }
}
