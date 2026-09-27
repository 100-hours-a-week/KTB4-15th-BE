package com.ktb.lookddak.global.client.ai.bodyimage.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class AiBodyImageValidationErrorResponse {

    private final Integer code;
    private final String message;
    private final AiBodyImageValidationErrorData data;

    @JsonCreator
    public AiBodyImageValidationErrorResponse(
            @JsonProperty("code") Integer code,
            @JsonProperty("message") String message,
            @JsonProperty("data") AiBodyImageValidationErrorData data
    ) {
        this.code = code;
        this.message = message;
        this.data = data;
    }
}
