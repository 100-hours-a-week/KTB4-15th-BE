package com.ktb.lookddak.domain.fitting.mq.outbox;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxEventSchedulerTest {

    private final OutboxEventClaimService claimService = mock(OutboxEventClaimService.class);
    private final OutboxEventPublisher publisher = mock(OutboxEventPublisher.class);

    @Test
    void doesNothingWhenSchedulerIsDisabled() {
        scheduler(false).publishPendingEvents();

        verify(claimService, never()).claimNextBatch(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void claimsAndPublishesEachClaimedEvent() {
        ClaimedOutboxEvent event = new ClaimedOutboxEvent(
                1L, 10L, "payload", "server-a", 0
        );
        when(claimService.claimNextBatch("server-a", 20, Duration.ofSeconds(30)))
                .thenReturn(List.of(event));

        scheduler(true).publishPendingEvents();

        verify(claimService).claimNextBatch("server-a", 20, Duration.ofSeconds(30));
        verify(publisher).publish(event);
    }

    private OutboxEventScheduler scheduler(boolean enabled) {
        return new OutboxEventScheduler(
                new FittingOutboxProperties(
                        enabled, "server-a", 20, Duration.ofSeconds(30)
                ),
                claimService,
                publisher
        );
    }
}
