package com.ktb.lookddak.domain.fitting.mq.outbox;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventRepositoryCustom {

    List<OutboxEvent> findClaimableForUpdateSkipLocked(
            LocalDateTime now,
            int batchSize
    );
}
