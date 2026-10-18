package com.ktb.lookddak.domain.fitting.mq;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "fitting.result-consumer")
public class FittingResultConsumerProperties {

    private final boolean enabled;
    @Min(1)
    @Max(10)
    private final int maxMessages;
    @Min(0)
    @Max(20)
    private final int waitTimeSeconds;
    @Min(0)
    @Max(43_200)
    private final int visibilityTimeoutSeconds;

    public FittingResultConsumerProperties(
            boolean enabled,
            int maxMessages,
            int waitTimeSeconds,
            int visibilityTimeoutSeconds
    ) {
        this.enabled = enabled;
        this.maxMessages = maxMessages;
        this.waitTimeSeconds = waitTimeSeconds;
        this.visibilityTimeoutSeconds = visibilityTimeoutSeconds;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getMaxMessages() {
        return maxMessages;
    }

    public int getWaitTimeSeconds() {
        return waitTimeSeconds;
    }

    public int getVisibilityTimeoutSeconds() {
        return visibilityTimeoutSeconds;
    }
}
