package com.ktb.lookddak.domain.chat.redis;

import com.ktb.lookddak.domain.chat.entity.ChatGenerationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatRedisStreamingServiceTest {

    private final ChatRedisSnapshotRepository snapshotRepository = mock(ChatRedisSnapshotRepository.class);
    private final ChatRedisEventPublisher eventPublisher = mock(ChatRedisEventPublisher.class);

    @Test
    @DisplayName("Redis 기능이 꺼져 있으면 기존 SSE 경로에 영향을 주지 않는다")
    void skipWhenDisabled() {
        ChatRedisStreamingService service = service(false);

        service.initialize(4365L);
        service.recordToken(4365L, "event-1", "추천");

        verify(snapshotRepository, never()).initialize(4365L);
        verify(snapshotRepository, never()).appendToken(4365L, "event-1", "추천");
        verify(eventPublisher, never()).publish(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("새 token이 Snapshot에 반영된 경우에만 Pub/Sub 이벤트를 발행한다")
    void publishOnlyAppliedToken() {
        ChatRedisStreamingService service = service(true);
        when(snapshotRepository.appendToken(4365L, "event-1", "추천"))
                .thenReturn(new ChatRedisSnapshotWriteResult(
                        7L, true, ChatGenerationStatus.GENERATING
                ));

        service.initialize(4365L);
        service.recordToken(4365L, "event-1", "추천");

        verify(snapshotRepository).initialize(4365L);
        verify(eventPublisher).publish(ChatRedisEvent.token(4365L, 7L, "추천"));
    }

    @Test
    @DisplayName("같은 eventId의 재시도는 Pub/Sub 이벤트를 중복 발행하지 않는다")
    void doNotPublishDuplicateToken() {
        ChatRedisStreamingService service = service(true);
        when(snapshotRepository.appendToken(4365L, "event-1", "추천"))
                .thenReturn(new ChatRedisSnapshotWriteResult(
                        7L, false, ChatGenerationStatus.GENERATING
                ));

        service.recordToken(4365L, "event-1", "추천");

        verify(eventPublisher, never()).publish(org.mockito.ArgumentMatchers.any());
    }

    private ChatRedisStreamingService service(boolean redisEnabled) {
        return new ChatRedisStreamingService(
                new ChatRedisProperties(redisEnabled, "lookddak:chat"),
                snapshotRepository,
                eventPublisher
        );
    }
}
