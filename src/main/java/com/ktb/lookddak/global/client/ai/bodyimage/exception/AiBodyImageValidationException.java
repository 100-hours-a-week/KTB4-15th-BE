package com.ktb.lookddak.global.client.ai.bodyimage.exception;

import lombok.Getter;

@Getter
public class AiBodyImageValidationException extends RuntimeException {

    private final String code;
    private final Integer httpStatus;
    private final String reasonCode;
    private final String reason;

    public AiBodyImageValidationException(
            String code,
            String message,
            Integer httpStatus,
            String reasonCode,
            String reason
    ) {
        this(code, message, httpStatus, reasonCode, reason, null);
    }

    public AiBodyImageValidationException(
            String code,
            String message,
            Integer httpStatus,
            String reasonCode,
            String reason,
            Throwable cause
    ) {
        super(message, cause);
        this.code = code;
        this.httpStatus = httpStatus;
        this.reasonCode = reasonCode;
        this.reason = reason;
    }
}
