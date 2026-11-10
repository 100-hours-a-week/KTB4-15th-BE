package com.ktb.lookddak.domain.chat.redis;

import com.ktb.lookddak.domain.chat.entity.ChatGenerationStatus;

/**
 * Redis Snapshot 변경 결과다.
 *
 * <p>applied=false이면 같은 eventId가 이미 반영된 재시도다. 호출자는 이 경우
 * Pub/Sub을 다시 발행하지 않아야 한다.</p>
 */
public record ChatRedisSnapshotWriteResult(
        long seq,
        boolean applied,
        ChatGenerationStatus status
) {
}
