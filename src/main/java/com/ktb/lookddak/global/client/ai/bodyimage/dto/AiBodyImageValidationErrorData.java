package com.ktb.lookddak.global.client.ai.bodyimage.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class AiBodyImageValidationErrorData {

    private final String reasonCode;
    private final String reason;

    @JsonCreator
    public AiBodyImageValidationErrorData(
            @JsonProperty("reason_code") String reasonCode,
            @JsonProperty("reason") String reason
    ) {
        this.reasonCode = reasonCode;
        this.reason = reason;
    }
}
