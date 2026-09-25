package com.ktb.lookddak.global.client.ai.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "ai.server")
public class AiClientProperties {

    @NotBlank
    private final String baseUrl;

    private final String apiKey;

    @NotNull
    private final Duration connectTimeout;

    @NotNull
    private final Duration responseTimeout;

    public AiClientProperties(
            String baseUrl,
            String apiKey,
            Duration connectTimeout,
            Duration responseTimeout
    ) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.connectTimeout = connectTimeout;
        this.responseTimeout = responseTimeout;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public Duration getResponseTimeout() {
        return responseTimeout;
    }
}
