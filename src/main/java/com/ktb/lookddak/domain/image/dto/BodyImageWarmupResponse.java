package com.ktb.lookddak.domain.image.dto;

import org.springframework.http.HttpStatus;

/**
 * 전신 이미지 검증 모델의 사전 활성화 요청 결과다.
 * 202는 준비 완료가 아닌 AI 서버의 비동기 요청 접수만 의미한다.
 */
public record BodyImageWarmupResponse(
        HttpStatus status,
        String code,
        String message,
        Long retryAfterSeconds
) {

    public static BodyImageWarmupResponse accepted(
            String code,
            String message
    ) {
        return new BodyImageWarmupResponse(
                HttpStatus.ACCEPTED,
                code,
                message,
                null
        );
    }

    public static BodyImageWarmupResponse rateLimited(
            String code,
            String message,
            Long retryAfterSeconds
    ) {
        return new BodyImageWarmupResponse(
                HttpStatus.TOO_MANY_REQUESTS,
                code,
                message,
                retryAfterSeconds
        );
    }

    public static BodyImageWarmupResponse unavailable(
            HttpStatus status,
            String code,
            String message
    ) {
        return new BodyImageWarmupResponse(status, code, message, null);
    }
}
