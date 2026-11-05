package com.ktb.lookddak.domain.chat.redis;

/** Redis Snapshot의 초기화 또는 상태 전이 규칙을 위반했을 때 발생한다. */
public class ChatRedisSnapshotWriteRejectedException extends RuntimeException {

    public ChatRedisSnapshotWriteRejectedException(String message) {
        super(message);
    }
}
