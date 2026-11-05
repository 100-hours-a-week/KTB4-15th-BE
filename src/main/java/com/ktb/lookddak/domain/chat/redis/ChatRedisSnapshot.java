package com.ktb.lookddak.domain.chat.redis;

import com.ktb.lookddak.domain.chat.entity.ChatGenerationStatus;

/**
 * 재연결 시 복구에 사용하는 Redis의 메시지별 누적 상태다.
 *
 * <p>content와 seq는 같은 Lua script로 읽는다. 따라서 reconnect 과정에서
 * 본문은 이전인데 순번만 미래인 식의 조합을 만들지 않는다.</p>
 */
public record ChatRedisSnapshot(
        Long userMessageId,
        String content,
        long seq,
        ChatGenerationStatus status
) {
}
