package com.ktb.lookddak.domain.fitting.mq.outbox;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxEventClaimServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Test
    void claimPendingEventsAndReturnImmutablePayloads() {
        OutboxEvent first = event(10L, 100L);
        OutboxEvent second = event(20L, 200L);
        given(outboxEventRepository.findClaimableForUpdateSkipLocked(
                any(LocalDateTime.class),
                eq(20)
        )).willReturn(List.of(first, second));

        OutboxEventClaimService service = new OutboxEventClaimService(
                outboxEventRepository
        );

        List<ClaimedOutboxEvent> claimedEvents = service.claimNextBatch(
                "server-a",
                20,
                Duration.ofSeconds(30)
        );

        assertThat(claimedEvents).containsExactly(
                new ClaimedOutboxEvent(10L, 100L, "{\"fitting_job_id\":100}"),
                new ClaimedOutboxEvent(20L, 200L, "{\"fitting_job_id\":200}")
        );
        assertThat(first.getStatus()).isEqualTo(OutboxEventStatus.PROCESSING);
        assertThat(second.getStatus()).isEqualTo(OutboxEventStatus.PROCESSING);
        assertThat(first.getClaimedBy()).isEqualTo("server-a");
        assertThat(first.getLeaseUntil()).isAfter(LocalDateTime.now());

        ArgumentCaptor<LocalDateTime> nowCaptor = ArgumentCaptor.forClass(
                LocalDateTime.class
        );
        verify(outboxEventRepository).findClaimableForUpdateSkipLocked(
                nowCaptor.capture(),
                eq(20)
        );
    }

    @Test
    void rejectInvalidClaimArguments() {
        OutboxEventClaimService service = new OutboxEventClaimService(
                outboxEventRepository
        );

        assertThatThrownBy(() -> service.claimNextBatch(
                "",
                20,
                Duration.ofSeconds(30)
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.claimNextBatch(
                "server-a",
                0,
                Duration.ofSeconds(30)
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.claimNextBatch(
                "server-a",
                20,
                Duration.ZERO
        )).isInstanceOf(IllegalArgumentException.class);
    }

    private OutboxEvent event(Long eventId, Long fittingJobId) {
        OutboxEvent event = OutboxEvent.fittingRequest(
                fittingJobId,
                "{\"fitting_job_id\":%d}".formatted(fittingJobId),
                LocalDateTime.now().minusMinutes(1)
        );
        org.springframework.test.util.ReflectionTestUtils.setField(
                event,
                "id",
                eventId
        );
        return event;
    }
}
