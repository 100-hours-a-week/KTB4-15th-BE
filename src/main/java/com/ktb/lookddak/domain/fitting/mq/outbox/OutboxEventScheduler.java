package com.ktb.lookddak.domain.fitting.mq.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventScheduler {

    private final FittingOutboxProperties properties;
    private final OutboxEventClaimService claimService;
    private final OutboxEventPublisher publisher;

    @Scheduled(fixedDelayString = "${fitting.outbox.poll-interval:1s}")
    public void publishPendingEvents() {
        if (!properties.isEnabled()) {
            return;
        }

        List<ClaimedOutboxEvent> events = claimService.claimNextBatch(
                properties.getServerId(),
                properties.getBatchSize(),
                properties.getLeaseDuration()
        );
        if (!events.isEmpty()) {
            log.debug("OUTBOX_EVENTS_CLAIMED serverId={} count={}",
                    properties.getServerId(), events.size());
        }
        events.forEach(publisher::publish);
    }
}
