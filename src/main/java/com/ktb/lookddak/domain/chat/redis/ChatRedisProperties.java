package com.ktb.lookddak.domain.chat.redis;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "chat.redis")
public class ChatRedisProperties {

    private final boolean enabled;
    @NotBlank
    private final String keyPrefix;
    private final int recoveryBufferMaxEvents;
    private final Duration generatingTtl;
    private final Duration terminalTtl;

    @ConstructorBinding
    public ChatRedisProperties(
            @DefaultValue("false") boolean enabled,
            @DefaultValue("lookddak:chat") String keyPrefix,
            @DefaultValue("512") int recoveryBufferMaxEvents,
            @DefaultValue("10m") Duration generatingTtl,
            @DefaultValue("5m") Duration terminalTtl
    ) {
        if (generatingTtl == null || generatingTtl.isZero() || generatingTtl.isNegative()) {
            throw new IllegalArgumentException("generatingTtl must be positive");
        }
        if (terminalTtl == null || terminalTtl.isZero() || terminalTtl.isNegative()) {
            throw new IllegalArgumentException("terminalTtl must be positive");
        }
        this.enabled = enabled;
        this.keyPrefix = keyPrefix;
        this.recoveryBufferMaxEvents = recoveryBufferMaxEvents;
        this.generatingTtl = generatingTtl;
        this.terminalTtl = terminalTtl;
    }

    public ChatRedisProperties(boolean enabled, String keyPrefix) {
        this(enabled, keyPrefix, 512, Duration.ofMinutes(10), Duration.ofMinutes(5));
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

    public Duration getGeneratingTtl() {
        return generatingTtl;
    }

    public Duration getTerminalTtl() {
        return terminalTtl;
    }
}
