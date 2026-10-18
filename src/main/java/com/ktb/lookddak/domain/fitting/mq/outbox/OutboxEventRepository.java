package com.ktb.lookddak.domain.fitting.mq.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long>,
        OutboxEventRepositoryCustom {
}
