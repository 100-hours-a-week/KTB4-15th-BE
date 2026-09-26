package com.ktb.lookddak.domain.image.exception;

import lombok.Getter;

@Getter
public class BodyImageValidationException extends RuntimeException {

    private final String code;

    public BodyImageValidationException(String code, String message) {
        super(message);
        this.code = code;
    }
}
