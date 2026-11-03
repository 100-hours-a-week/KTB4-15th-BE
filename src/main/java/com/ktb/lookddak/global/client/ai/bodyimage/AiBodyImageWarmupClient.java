package com.ktb.lookddak.global.client.ai.bodyimage;

import com.ktb.lookddak.global.client.ai.config.AiClientProperties;
import com.ktb.lookddak.global.client.ai.dto.AiErrorResponse;
import com.ktb.lookddak.global.client.ai.exception.AiClientException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.TimeoutException;

@Component
public class AiBodyImageWarmupClient {

    private static final String WARMUP_PATH =
            "/api/v1/validation/body-image/warmup";
    private static final String WARMUP_ACCEPTED_CODE =
            "BODY_IMAGE_WARMUP_ACCEPTED";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final AiClientProperties properties;

    public AiBodyImageWarmupClient(
            @Qualifier("aiWebClient") WebClient webClient,
            ObjectMapper objectMapper,
            AiClientProperties properties
    ) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public AiBodyImageWarmupResult requestWarmup() {
        AiErrorResponse response = webClient.post()
                .uri(WARMUP_PATH)
                .retrieve()
                .onStatus(
                        status -> status.value() != HttpStatus.ACCEPTED.value(),
                        clientResponse -> {
                            int status = clientResponse.statusCode().value();
                            Long retryAfterSeconds = retryAfterSeconds(
                                    clientResponse.headers().asHttpHeaders()
                            );
                            return clientResponse.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .map(body -> parseErrorResponse(
                                            status,
                                            body,
                                            retryAfterSeconds
                                    ));
                        }
                )
                .bodyToMono(AiErrorResponse.class)
                .timeout(properties.getResponseTimeout())
                .onErrorMap(
                        TimeoutException.class,
                        this::createTimeoutException
                )
                .onErrorMap(
                        WebClientRequestException.class,
                        this::createRequestException
                )
                .block();

        if (response == null
                || !WARMUP_ACCEPTED_CODE.equals(response.getCode())) {
            throw new AiClientException(
                    "AI_INVALID_RESPONSE",
                    "AI Warm-up 응답이 올바르지 않습니다.",
                    HttpStatus.ACCEPTED.value()
            );
        }

        return new AiBodyImageWarmupResult(
                response.getCode(),
                response.getMessage()
        );
    }

    private AiClientException parseErrorResponse(
            int httpStatus,
            String body,
            Long retryAfterSeconds
    ) {
        if (!StringUtils.hasText(body)) {
            return new AiClientException(
                    "AI_HTTP_ERROR",
                    "AI 서버가 오류를 반환했습니다.",
                    httpStatus,
                    retryAfterSeconds,
                    null
            );
        }

        try {
            AiErrorResponse response = objectMapper.readValue(
                    body,
                    AiErrorResponse.class
            );
            return new AiClientException(
                    response.getCode(),
                    response.getMessage(),
                    httpStatus,
                    retryAfterSeconds,
                    null
            );
        } catch (JacksonException exception) {
            return new AiClientException(
                    "AI_INVALID_RESPONSE",
                    "AI 오류 응답 형식을 해석할 수 없습니다.",
                    httpStatus,
                    retryAfterSeconds,
                    exception
            );
        }
    }

    private Long retryAfterSeconds(HttpHeaders headers) {
        String value = headers.getFirst(HttpHeaders.RETRY_AFTER);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            long seconds = Long.parseLong(value);
            return seconds >= 0 ? seconds : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private AiClientException createTimeoutException(Throwable exception) {
        return new AiClientException(
                "AI_RESPONSE_TIMEOUT",
                "AI Warm-up 응답 시간을 초과했습니다.",
                null,
                exception
        );
    }

    private AiClientException createRequestException(
            WebClientRequestException exception
    ) {
        return new AiClientException(
                "AI_SERVER_UNAVAILABLE",
                "AI 서버에 연결할 수 없습니다.",
                null,
                exception
        );
    }

    public record AiBodyImageWarmupResult(String code, String message) {
    }
}
