package com.ktb.lookddak.domain.chat.redis;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "chat.redis")
public class ChatRedisProperties {

    private final boolean enabled;
    @NotBlank
    private final String keyPrefix;
    private final int recoveryBufferMaxEvents;

    @ConstructorBinding
    public ChatRedisProperties(
            @DefaultValue("false") boolean enabled,
            @DefaultValue("lookddak:chat") String keyPrefix,
            @DefaultValue("512") int recoveryBufferMaxEvents
    ) {
        this.enabled = enabled;
        this.keyPrefix = keyPrefix;
        this.recoveryBufferMaxEvents = recoveryBufferMaxEvents;
    }

    public ChatRedisProperties(boolean enabled, String keyPrefix) {
        this(enabled, keyPrefix, 512);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public int getRecoveryBufferMaxEvents() {
        return recoveryBufferMaxEvents;
    }
}
