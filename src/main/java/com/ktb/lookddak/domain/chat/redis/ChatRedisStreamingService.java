package com.ktb.lookddak.domain.chat.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * AI token을 Redis Snapshot과 Pub/Sub으로 전달하는 F-3 진입점이다.
 *
 * <p>기능 플래그가 꺼진 환경에서는 완전한 no-op이다. 기존 JVM 메모리 기반 SSE 흐름을
 * 변경하지 않은 채 Redis 경로를 단계적으로 활성화하기 위해서다.</p>
 */
@Service
@RequiredArgsConstructor
public class ChatRedisStreamingService {

    private final ChatRedisProperties properties;
    private final ChatRedisSnapshotRepository snapshotRepository;
    private final ChatRedisEventPublisher eventPublisher;

    public void initialize(Long userMessageId) {
        if (!properties.isEnabled()) {
            return;
        }
        snapshotRepository.initialize(userMessageId);
    }

    /**
     * Snapshot 반영이 최초 성공한 경우에만 실시간 이벤트를 발행한다.
     * 같은 eventId로 인한 재시도는 Redis가 기존 seq를 반환하고 여기서는 publish하지 않는다.
     */
    public void recordToken(Long userMessageId, String eventId, String content) {
        if (!properties.isEnabled()) {
            return;
        }

        ChatRedisSnapshotWriteResult result = snapshotRepository.appendToken(
                userMessageId,
                eventId,
                content
        );
        if (result.applied()) {
            eventPublisher.publish(ChatRedisEvent.token(
                    userMessageId,
                    result.seq(),
                    content
            ));
        }
    }
}
