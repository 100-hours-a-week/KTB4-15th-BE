package com.ktb.lookddak.global.client.ai.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "ai.task")
public class AiTaskProperties {

    @Min(1)
    private final int corePoolSize;

    @Min(1)
    private final int maxPoolSize;

    @Min(0)
    private final int queueCapacity;

    @NotNull
    private final Duration awaitTermination;

    public AiTaskProperties(
            int corePoolSize,
            int maxPoolSize,
            int queueCapacity,
            Duration awaitTermination
    ) {
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.queueCapacity = queueCapacity;
        this.awaitTermination = awaitTermination;
    }

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public Duration getAwaitTermination() {
        return awaitTermination;
    }
}
