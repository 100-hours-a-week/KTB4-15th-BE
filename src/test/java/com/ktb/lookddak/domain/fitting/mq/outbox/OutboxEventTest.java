package com.ktb.lookddak.domain.fitting.mq.outbox;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxEventTest {

    private static final LocalDateTime NOW = LocalDateTime.of(
            2026, 10, 18, 12, 0
    );

    @Test
    void createFittingRequestAsPendingEvent() {
        OutboxEvent event = OutboxEvent.fittingRequest(
                123L,
                "{\"fitting_job_id\":123}",
                NOW
        );

        assertThat(event.getEventType()).isEqualTo(OutboxEventType.FITTING_REQUEST);
        assertThat(event.getAggregateId()).isEqualTo(123L);
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(event.getRetryCount()).isZero();
        assertThat(event.getNextRetryAt()).isEqualTo(NOW);
        assertThat(event.isClaimableAt(NOW)).isTrue();
    }

    @Test
    void claimAndPublishEvent() {
        OutboxEvent event = createEvent();
        LocalDateTime leaseUntil = NOW.plusMinutes(1);

        event.claim("server-a", NOW, leaseUntil);

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PROCESSING);
        assertThat(event.getClaimedBy()).isEqualTo("server-a");
        assertThat(event.isClaimableAt(NOW.plusSeconds(30))).isFalse();

        event.markPublished(NOW.plusSeconds(5));

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
        assertThat(event.getPublishedAt()).isEqualTo(NOW.plusSeconds(5));
        assertThat(event.getClaimedBy()).isNull();
        assertThat(event.getLeaseUntil()).isNull();
    }

    @Test
    void returnToPendingAfterPublishFailure() {
        OutboxEvent event = createEvent();
        event.claim("server-a", NOW, NOW.plusMinutes(1));

        event.markPublishFailed(
                NOW.plusMinutes(2),
                "SQS connection timeout"
        );

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getNextRetryAt()).isEqualTo(NOW.plusMinutes(2));
        assertThat(event.getLastError()).isEqualTo("SQS connection timeout");
        assertThat(event.getClaimedBy()).isNull();
    }

    @Test
    void expiredProcessingEventCanBeClaimedAgain() {
        OutboxEvent event = createEvent();
        event.claim("server-a", NOW, NOW.plusMinutes(1));

        assertThat(event.isClaimableAt(NOW.plusMinutes(1))).isTrue();

        event.claim("server-b", NOW.plusMinutes(1), NOW.plusMinutes(2));

        assertThat(event.getClaimedBy()).isEqualTo("server-b");
        assertThat(event.getLeaseUntil()).isEqualTo(NOW.plusMinutes(2));
    }

    @Test
    void cannotReflectPublishResultWithoutClaim() {
        OutboxEvent event = createEvent();

        assertThatThrownBy(() -> event.markPublished(NOW))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cannotClaimProcessingEventBeforeLeaseExpires() {
        OutboxEvent event = createEvent();
        event.claim("server-a", NOW, NOW.plusMinutes(1));

        assertThatThrownBy(() -> event.claim(
                "server-b",
                NOW.plusSeconds(30),
                NOW.plusMinutes(2)
        )).isInstanceOf(IllegalStateException.class);
    }

    private OutboxEvent createEvent() {
        return OutboxEvent.fittingRequest(
                123L,
                "{\"fitting_job_id\":123}",
                NOW
        );
    }
}
