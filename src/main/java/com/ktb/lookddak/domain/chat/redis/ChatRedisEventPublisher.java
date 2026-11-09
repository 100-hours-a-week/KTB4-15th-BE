package com.ktb.lookddak.domain.chat.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** Redis Pub/Sub 채널에 채팅 이벤트를 JSON으로 발행한다. */
@Component
@RequiredArgsConstructor
public class ChatRedisEventPublisher {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ChatRedisKeyFactory keyFactory;

    public void publish(ChatRedisEvent event) {
        try {
            redisTemplate.convertAndSend(
                    keyFactory.eventChannel(event.userMessageId()),
                    objectMapper.writeValueAsString(event)
            );
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Failed to serialize Redis chat event userMessageId=" + event.userMessageId(),
                    exception
            );
        }
    }
}
