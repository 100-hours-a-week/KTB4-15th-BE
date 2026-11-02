package com.ktb.lookddak.domain.chat.redis;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/** Redis 기능이 켜진 BE만 모든 채팅 이벤트 채널을 pattern 구독한다. */
@Configuration
public class ChatRedisPubSubConfig {

    @Bean
    @ConditionalOnProperty(prefix = "chat.redis", name = "enabled", havingValue = "true")
    public RedisMessageListenerContainer chatRedisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            ChatRedisEventSubscriber chatRedisEventSubscriber,
            ChatRedisKeyFactory keyFactory
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(
                chatRedisEventSubscriber,
                new PatternTopic(keyFactory.eventChannelPattern())
        );
        return container;
    }
}
