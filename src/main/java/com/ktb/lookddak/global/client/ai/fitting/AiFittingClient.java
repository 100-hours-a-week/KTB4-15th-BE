package com.ktb.lookddak.global.client.ai.fitting;

import com.ktb.lookddak.global.client.ai.config.AiClientProperties;
import com.ktb.lookddak.global.client.ai.fitting.dto.AiFittingRequest;
import com.ktb.lookddak.global.client.ai.fitting.dto.AiFittingResponse;
import com.ktb.lookddak.global.client.ai.fitting.dto.AiFittingResultData;
import com.ktb.lookddak.global.client.ai.fitting.exception.AiFittingException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.TimeoutException;

@Component
public class AiFittingClient {

    private static final String FITTING_PATH = "/api/v1/sync-fitting";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final AiClientProperties properties;

    public AiFittingClient(
            @Qualifier("aiWebClient") WebClient webClient,
            ObjectMapper objectMapper,
            AiClientProperties properties
    ) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public AiFittingResultData requestFitting(AiFittingRequest request) {
        AiFittingResponse response = webClient.post()
                .uri(FITTING_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.isError(), clientResponse -> {
                    int status = clientResponse.statusCode().value();
                    return clientResponse.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .map(body -> parseErrorResponse(status, body));
                })
                .bodyToMono(AiFittingResponse.class)
                .timeout(properties.getResponseTimeout())
                .onErrorMap(
                        TimeoutException.class,
                        this::createTimeoutException
                )
                .onErrorMap(
                        WebClientRequestException.class,
                        this::createRequestException
                )
                .onErrorMap(
                        exception -> !(exception instanceof AiFittingException),
                        this::createInvalidResponseException
                )
                .block();

        return validateSuccessResponse(response);
    }

    private AiFittingException parseErrorResponse(
            int httpStatus,
            String body
    ) {
        if (!StringUtils.hasText(body)) {
            return new AiFittingException(
                    "AI_HTTP_ERROR",
                    "AI 가상피팅 서버가 오류를 반환했습니다.",
                    httpStatus
            );
        }

        try {
            AiFittingResponse response = objectMapper.readValue(
                    body,
                    AiFittingResponse.class
            );
            String code = StringUtils.hasText(response.getMessage())
                    ? response.getMessage()
                    : "AI_HTTP_ERROR";
            return new AiFittingException(
                    code,
                    "AI 가상피팅 요청이 실패했습니다.",
                    httpStatus
            );
        } catch (JacksonException exception) {
            return new AiFittingException(
                    "AI_INVALID_RESPONSE",
                    "AI 가상피팅 오류 응답을 해석할 수 없습니다.",
                    httpStatus,
                    exception
            );
        }
    }

    private AiFittingResultData validateSuccessResponse(
            AiFittingResponse response
    ) {
        AiFittingResultData data = response == null
                ? null
                : response.getData();
        if (response == null
                || response.getCode() == null
                || response.getCode() != 200
                || !"fitting_succeeded".equals(response.getMessage())
                || data == null
                || !StringUtils.hasText(data.getResultImageKey())
                || !StringUtils.hasText(data.getLlmTitle())
                || !StringUtils.hasText(data.getLlmComment())) {
            throw new AiFittingException(
                    "AI_INVALID_RESPONSE",
                    "AI 가상피팅 성공 응답이 올바르지 않습니다.",
                    null
            );
        }
        return data;
    }

    private AiFittingException createTimeoutException(Throwable exception) {
        return new AiFittingException(
                "AI_RESPONSE_TIMEOUT",
                "AI 가상피팅 응답 시간을 초과했습니다.",
                null,
                exception
        );
    }

    private AiFittingException createRequestException(
            WebClientRequestException exception
    ) {
        if (isTimeout(exception)) {
            return createTimeoutException(exception);
        }
        return new AiFittingException(
                "AI_SERVER_UNAVAILABLE",
                "AI 가상피팅 서버에 연결할 수 없습니다.",
                null,
                exception
        );
    }

    private AiFittingException createInvalidResponseException(
            Throwable exception
    ) {
        return new AiFittingException(
                "AI_INVALID_RESPONSE",
                "AI 가상피팅 응답을 처리할 수 없습니다.",
                null,
                exception
        );
    }

    private boolean isTimeout(Throwable exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof TimeoutException
                    || cause.getClass().getSimpleName().contains("Timeout")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
