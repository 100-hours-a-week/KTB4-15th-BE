package com.ktb.lookddak.domain.fitting.mq.outbox;

public record ClaimedOutboxEvent(
        Long outboxEventId,
        Long fittingJobId,
        String payload,
        String claimedBy,
        int retryCount
) {

    public static ClaimedOutboxEvent from(OutboxEvent event) {
        return new ClaimedOutboxEvent(
                event.getId(),
                event.getAggregateId(),
                event.getPayload(),
                event.getClaimedBy(),
                event.getRetryCount()
        );
    }
}
