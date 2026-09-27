package com.ktb.lookddak.global.client.ai.fitting.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class AiFittingResponse {

    private final Integer code;
    private final String message;
    private final AiFittingResultData data;

    @JsonCreator
    public AiFittingResponse(
            @JsonProperty("code") Integer code,
            @JsonProperty("message") String message,
            @JsonProperty("data") AiFittingResultData data
    ) {
        this.code = code;
        this.message = message;
        this.data = data;
    }
}
