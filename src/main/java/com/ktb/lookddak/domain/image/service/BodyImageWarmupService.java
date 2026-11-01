package com.ktb.lookddak.domain.image.service;

import com.ktb.lookddak.domain.image.dto.BodyImageWarmupResponse;
import com.ktb.lookddak.global.client.ai.bodyimage.AiBodyImageWarmupClient;
import com.ktb.lookddak.global.client.ai.exception.AiClientException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class BodyImageWarmupService {

    private static final String RATE_LIMITED_CODE =
            "BODY_IMAGE_WARMUP_RATE_LIMITED";
    private static final String UNAVAILABLE_CODE =
            "BODY_IMAGE_WARMUP_UNAVAILABLE";

    private final AiBodyImageWarmupClient warmupClient;

    public BodyImageWarmupService(AiBodyImageWarmupClient warmupClient) {
        this.warmupClient = warmupClient;
    }

    public BodyImageWarmupResponse requestWarmup() {
        try {
            AiBodyImageWarmupClient.AiBodyImageWarmupResult result =
                    warmupClient.requestWarmup();
            return BodyImageWarmupResponse.accepted(
                    result.code(),
                    result.message()
            );
        } catch (AiClientException exception) {
            return mapFailure(exception);
        }
    }

    private BodyImageWarmupResponse mapFailure(AiClientException exception) {
        if (Integer.valueOf(429).equals(exception.getHttpStatus())
                && RATE_LIMITED_CODE.equals(exception.getCode())) {
            return BodyImageWarmupResponse.rateLimited(
                    RATE_LIMITED_CODE,
                    "사전 활성화 요청이 너무 많습니다. 잠시 후 다시 시도해주세요.",
                    exception.getRetryAfterSeconds()
            );
        }

        if ("AI_RESPONSE_TIMEOUT".equals(exception.getCode())) {
            return BodyImageWarmupResponse.unavailable(
                    HttpStatus.GATEWAY_TIMEOUT,
                    UNAVAILABLE_CODE,
                    "현재 사전 활성화 요청을 처리할 수 없습니다."
            );
        }

        HttpStatus status = isServiceUnavailable(exception)
                ? HttpStatus.SERVICE_UNAVAILABLE
                : HttpStatus.BAD_GATEWAY;
        return BodyImageWarmupResponse.unavailable(
                status,
                UNAVAILABLE_CODE,
                "현재 사전 활성화 요청을 처리할 수 없습니다."
        );
    }

    private boolean isServiceUnavailable(AiClientException exception) {
        return Integer.valueOf(401).equals(exception.getHttpStatus())
                || Integer.valueOf(503).equals(exception.getHttpStatus());
    }
}
