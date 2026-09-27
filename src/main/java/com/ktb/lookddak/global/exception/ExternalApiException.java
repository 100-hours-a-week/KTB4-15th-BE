package com.ktb.lookddak.global.exception;

import lombok.Getter;

@Getter
public class ExternalApiException extends RuntimeException {

    private final ErrorCode errorCode;

    public ExternalApiException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}
