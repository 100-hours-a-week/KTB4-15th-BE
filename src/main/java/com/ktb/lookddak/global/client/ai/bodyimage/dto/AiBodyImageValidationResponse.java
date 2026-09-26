package com.ktb.lookddak.global.client.ai.bodyimage.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class AiBodyImageValidationResponse {

    private final Integer code;
    private final String message;
    private final AiBodyImageValidationData data;

    @JsonCreator
    public AiBodyImageValidationResponse(
            @JsonProperty("code") Integer code,
            @JsonProperty("message") String message,
            @JsonProperty("data") AiBodyImageValidationData data
    ) {
        this.code = code;
        this.message = message;
        this.data = data;
    }
}
