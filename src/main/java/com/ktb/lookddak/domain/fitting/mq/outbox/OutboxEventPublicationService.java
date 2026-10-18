package com.ktb.lookddak.domain.fitting.mq.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OutboxEventPublicationService {

    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public void markPublished(Long outboxEventId, String serverId) {
        OutboxEvent event = getOwnedProcessingEvent(outboxEventId, serverId);
        if (event != null) {
            event.markPublished(LocalDateTime.now());
        }
    }

    @Transactional
    public void markPublishFailed(
            Long outboxEventId,
            String serverId,
            LocalDateTime nextRetryAt,
            String lastError
    ) {
        OutboxEvent event = getOwnedProcessingEvent(outboxEventId, serverId);
        if (event != null) {
            event.markPublishFailed(nextRetryAt, lastError);
        }
    }

    private OutboxEvent getOwnedProcessingEvent(
            Long outboxEventId,
            String serverId
    ) {
        return outboxEventRepository.findByIdForUpdate(outboxEventId)
                .filter(event -> event.getStatus() == OutboxEventStatus.PROCESSING)
                .filter(event -> serverId.equals(event.getClaimedBy()))
                .orElse(null);
    }
}
