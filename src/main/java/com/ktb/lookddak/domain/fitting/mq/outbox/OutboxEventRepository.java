package com.ktb.lookddak.domain.fitting.mq.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long>,
        OutboxEventRepositoryCustom {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select outboxEvent
            from OutboxEvent outboxEvent
            where outboxEvent.id = :outboxEventId
            """)
    Optional<OutboxEvent> findByIdForUpdate(
            @Param("outboxEventId") Long outboxEventId
    );
}
