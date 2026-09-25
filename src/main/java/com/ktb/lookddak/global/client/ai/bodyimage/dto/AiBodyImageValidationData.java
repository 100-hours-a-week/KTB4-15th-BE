package com.ktb.lookddak.global.client.ai.bodyimage.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class AiBodyImageValidationData {

    private final String s3Key;
    private final List<String> warnings;

    @JsonCreator
    public AiBodyImageValidationData(
            @JsonProperty("s3_key") String s3Key,
            @JsonProperty("warnings") List<String> warnings
    ) {
        this.s3Key = s3Key;
        this.warnings = warnings == null
                ? List.of()
                : List.copyOf(warnings);
    }
}
