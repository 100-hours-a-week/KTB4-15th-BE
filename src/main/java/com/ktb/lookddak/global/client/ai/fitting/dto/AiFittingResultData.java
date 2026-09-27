package com.ktb.lookddak.global.client.ai.fitting.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class AiFittingResultData {

    private final String resultImageKey;
    private final String llmTitle;
    private final String llmComment;

    @JsonCreator
    public AiFittingResultData(
            @JsonProperty("result_image_key") String resultImageKey,
            @JsonProperty("llm_title") String llmTitle,
            @JsonProperty("llm_comment") String llmComment
    ) {
        this.resultImageKey = resultImageKey;
        this.llmTitle = llmTitle;
        this.llmComment = llmComment;
    }
}
