package com.ktb.lookddak.global.client.ai.bodyimage.exception;

import lombok.Getter;

@Getter
public class AiBodyImageValidationException extends RuntimeException {

    private final String code;
    private final Integer httpStatus;

    public AiBodyImageValidationException(
            String code,
            String message,
            Integer httpStatus
    ) {
        this(code, message, httpStatus, null);
    }

    public AiBodyImageValidationException(
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
