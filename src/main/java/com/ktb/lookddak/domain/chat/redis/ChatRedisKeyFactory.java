package com.ktb.lookddak.domain.chat.redis;

import org.springframework.stereotype.Component;

/**
 * 메시지별 Redis 키와 Pub/Sub 채널명을 한 곳에서 관리한다.
 * content/state 키의 hash tag를 같게 해 이후 Lua script가 같은 Cluster 슬롯에서
 * 두 키를 원자적으로 갱신·조회할 수 있게 한다.
 */
@Component
public class ChatRedisKeyFactory {

    private final String keyPrefix;

    public ChatRedisKeyFactory(ChatRedisProperties properties) {
        this.keyPrefix = properties.getKeyPrefix();
    }

    public String contentKey(Long userMessageId) {
        return messagePrefix(userMessageId) + ":content";
    }

    public String stateKey(Long userMessageId) {
        return messagePrefix(userMessageId) + ":state";
    }

    public String eventChannel(Long userMessageId) {
        return keyPrefix + ":events:" + userMessageId;
    }

    public String eventChannelPattern() {
        return keyPrefix + ":events:*";
    }

    /**
     * 같은 AI 이벤트의 Redis 쓰기 재시도를 식별하는 키다.
     * messageId hash tag를 유지해 Snapshot 상태와 한 Lua script에서 다룬다.
     */
    public String appliedEventKey(Long userMessageId, String eventId) {
        return messagePrefix(userMessageId) + ":event:" + eventId;
    }

    private String messagePrefix(Long userMessageId) {
        return keyPrefix + ":{" + userMessageId + "}";
    }
}
