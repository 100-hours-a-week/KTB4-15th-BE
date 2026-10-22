package com.ktb.lookddak.domain.fitting.mq.outbox;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class OutboxEventRepositoryImpl implements OutboxEventRepositoryCustom {

    private final EntityManager entityManager;

    @Override
    @SuppressWarnings("unchecked")
    public List<OutboxEvent> findClaimableForUpdateSkipLocked(
            LocalDateTime now,
            int batchSize
    ) {
        return entityManager.createNativeQuery("""
                select *
                from outbox_event
                where (
                    status = 'PENDING'
                    and next_retry_at <= :now
                )
                or (
                    status = 'PROCESSING'
                    and lease_until <= :now
                )
                order by id
                limit :batchSize
                for update skip locked
                """, OutboxEvent.class)
                .setParameter("now", now)
                .setParameter("batchSize", batchSize)
                .getResultList();
    }
}
