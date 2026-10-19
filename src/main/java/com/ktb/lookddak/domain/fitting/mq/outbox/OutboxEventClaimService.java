package com.ktb.lookddak.domain.fitting.mq.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxEventClaimService {

    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public List<ClaimedOutboxEvent> claimNextBatch(
            String serverId,
            int batchSize,
            Duration leaseDuration
    ) {
        validateClaimArguments(serverId, batchSize, leaseDuration);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime leaseUntil = now.plus(leaseDuration);
        List<OutboxEvent> events = outboxEventRepository
                .findClaimableForUpdateSkipLocked(now, batchSize);

        List<ClaimedOutboxEvent> claimedEvents = new ArrayList<>();
        for (OutboxEvent event : events) {
            event.claim(serverId, now, leaseUntil);
            claimedEvents.add(ClaimedOutboxEvent.from(event));
        }
        return List.copyOf(claimedEvents);
    }

    private void validateClaimArguments(
            String serverId,
            int batchSize,
            Duration leaseDuration
    ) {
        if (serverId == null || serverId.isBlank()) {
            throw new IllegalArgumentException("Outbox Claim 서버 ID가 필요합니다.");
        }
        if (batchSize <= 0) {
            throw new IllegalArgumentException("Outbox Claim batchSize는 양수여야 합니다.");
        }
        if (leaseDuration == null || leaseDuration.isNegative()
                || leaseDuration.isZero()) {
            throw new IllegalArgumentException("Outbox lease는 양수여야 합니다.");
        }
    }
}
