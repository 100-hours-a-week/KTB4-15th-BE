package com.ktb.lookddak.global.client.ai.bodyimage.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class AiBodyImageValidationData {

    private final String s3Key;

    @JsonCreator
    public AiBodyImageValidationData(
            @JsonProperty("s3_key") String s3Key
    ) {
        this.s3Key = s3Key;
    }
}
