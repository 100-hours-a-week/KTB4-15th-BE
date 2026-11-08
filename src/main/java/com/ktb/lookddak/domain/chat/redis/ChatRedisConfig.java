package com.ktb.lookddak.domain.chat.redis;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ChatRedisProperties.class)
public class ChatRedisConfig {
}
