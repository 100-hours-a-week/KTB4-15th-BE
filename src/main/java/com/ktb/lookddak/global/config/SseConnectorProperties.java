package com.ktb.lookddak.global.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "server")
public class SseConnectorProperties {

    @Min(1)
    @Max(65535)
    private final int ssePort;

    public SseConnectorProperties(@DefaultValue("8081") int ssePort) {
        this.ssePort = ssePort;
    }

    public int getSsePort() {
        return ssePort;
    }
}
