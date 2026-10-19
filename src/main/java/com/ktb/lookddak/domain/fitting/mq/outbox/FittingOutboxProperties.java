package com.ktb.lookddak.domain.fitting.mq.outbox;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "fitting.outbox")
public class FittingOutboxProperties {

    private final boolean enabled;
    @NotBlank
    private final String serverId;
    @Positive
    private final int batchSize;
    private final Duration leaseDuration;

    public FittingOutboxProperties(
            boolean enabled,
            String serverId,
            int batchSize,
            Duration leaseDuration
    ) {
        this.enabled = enabled;
        this.serverId = serverId;
        this.batchSize = batchSize;
        this.leaseDuration = leaseDuration;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getServerId() {
        return serverId;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public Duration getLeaseDuration() {
        return leaseDuration;
    }
}
