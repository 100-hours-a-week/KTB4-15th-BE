package com.ktb.lookddak.domain.fitting.mq.outbox;

import com.ktb.lookddak.global.config.JpaConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaConfig.class)
class OutboxEventRepositoryTest {

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findPendingAndExpiredProcessingEventsWithSkipLocked() {
        LocalDateTime now = LocalDateTime.now();
        OutboxEvent pending = outboxEventRepository.save(
                OutboxEvent.fittingRequest(
                        100L,
                        "{\"fitting_job_id\":100}",
                        now.minusMinutes(1)
                )
        );
        OutboxEvent retryLater = outboxEventRepository.save(
                OutboxEvent.fittingRequest(
                        200L,
                        "{\"fitting_job_id\":200}",
                        now.plusMinutes(1)
                )
        );
        OutboxEvent expiredProcessing = outboxEventRepository.save(
                OutboxEvent.fittingRequest(
                        300L,
                        "{\"fitting_job_id\":300}",
                        now.minusMinutes(3)
                )
        );
        expiredProcessing.claim(
                "server-a",
                now.minusMinutes(2),
                now.minusMinutes(1)
        );
        OutboxEvent processing = outboxEventRepository.save(
                OutboxEvent.fittingRequest(
                        400L,
                        "{\"fitting_job_id\":400}",
                        now.minusMinutes(3)
                )
        );
        processing.claim(
                "server-a",
                now.minusMinutes(2),
                now.plusMinutes(1)
        );
        outboxEventRepository.flush();
        entityManager.clear();

        List<OutboxEvent> events = outboxEventRepository
                .findClaimableForUpdateSkipLocked(now, 10);

        assertThat(events)
                .extracting(OutboxEvent::getId)
                .containsExactly(pending.getId(), expiredProcessing.getId());
        assertThat(events)
                .extracting(OutboxEvent::getId)
                .doesNotContain(retryLater.getId(), processing.getId());
    }
}
