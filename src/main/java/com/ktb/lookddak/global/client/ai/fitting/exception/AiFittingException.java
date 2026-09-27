package com.ktb.lookddak.global.client.ai.fitting.exception;

import lombok.Getter;

@Getter
public class AiFittingException extends RuntimeException {

    private final String code;
    private final Integer httpStatus;

    public AiFittingException(
            String code,
            String message,
            Integer httpStatus
    ) {
        this(code, message, httpStatus, null);
    }

    public AiFittingException(
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
