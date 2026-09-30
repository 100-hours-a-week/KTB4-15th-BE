package com.ktb.lookddak.global.client.ai.exception;

import lombok.Getter;

@Getter
public class AiClientException extends RuntimeException {

    private final String code;
    private final Integer httpStatus;

    public AiClientException(
            String code,
            String message,
            Integer httpStatus
    ) {
        this(code, message, httpStatus, null);
    }

    public AiClientException(
            String code,
            String message,
            Integer httpStatus,
            Throwable cause
    ) {
        super(message, cause);
        this.code = code;
        this.httpStatus = httpStatus;
    }
}
