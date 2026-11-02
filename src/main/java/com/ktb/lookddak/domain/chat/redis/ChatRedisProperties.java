package com.ktb.lookddak.domain.chat.redis;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "chat.redis")
public class ChatRedisProperties {

    private final boolean enabled;
    @NotBlank
    private final String keyPrefix;

    public ChatRedisProperties(
            @DefaultValue("false") boolean enabled,
            @DefaultValue("lookddak:chat") String keyPrefix
    ) {
        this.enabled = enabled;
        this.keyPrefix = keyPrefix;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }
}
