package com.ktb.lookddak.global.client.ai.exception;

import lombok.Getter;

@Getter
public class AiClientException extends RuntimeException {

    private final String code;
    private final Integer httpStatus;
    private final Long retryAfterSeconds;

    public AiClientException(
            String code,
            String message,
            Integer httpStatus
    ) {
        this(code, message, httpStatus, null, null);
    }

    public AiClientException(
            String code,
            String message,
            Integer httpStatus,
            Throwable cause
    ) {
        this(code, message, httpStatus, null, cause);
    }

    public AiClientException(
            String code,
            String message,
            Integer httpStatus,
            Long retryAfterSeconds,
            Throwable cause
    ) {
        super(message, cause);
        this.code = code;
        this.httpStatus = httpStatus;
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
