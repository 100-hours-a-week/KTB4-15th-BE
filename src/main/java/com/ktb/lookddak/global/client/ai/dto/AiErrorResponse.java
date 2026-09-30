package com.ktb.lookddak.global.client.ai.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class AiErrorResponse {

    private final String code;
    private final Object data;
    private final String message;

    @JsonCreator
    public AiErrorResponse(
            @JsonProperty("code") String code,
            @JsonProperty("data") Object data,
            @JsonProperty("message") String message
    ) {
        this.code = code;
        this.data = data;
        this.message = message;
    }
}
