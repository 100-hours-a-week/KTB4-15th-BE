package com.ktb.lookddak.global.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.ktb.lookddak.global.exception.ErrorCode;
import lombok.Getter;

@Getter
@JsonPropertyOrder({"code", "data", "message"})
public class ApiResponse<T> {

    private final String code;
    private final T data;
    private final String message;

    private ApiResponse(String code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public static <T> ApiResponse<T> success(SuccessCode successCode, T data) {
        return new ApiResponse<>(successCode.name(), data, successCode.getMessage());
    }

    public static ApiResponse<Void> failure(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.name(), null, errorCode.getMessage());
    }
}
