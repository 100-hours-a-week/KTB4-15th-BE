package com.ktb.lookddak.domain.fitting.mq.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;

@Getter
@JsonPropertyOrder({
        "fitting_job_id",
        "status",
        "result_image_key",
        "llm_title",
        "llm_comment",
        "error_code",
        "error_message"
})
public class FittingResultMessage {

    private final Long fittingJobId;
    private final FittingResultStatus status;
    private final String resultImageKey;
    private final String llmTitle;
    private final String llmComment;
    private final String errorCode;
    private final String errorMessage;

    @JsonCreator
    public FittingResultMessage(
            @JsonProperty("fitting_job_id") Long fittingJobId,
            @JsonProperty("status") FittingResultStatus status,
            @JsonProperty("result_image_key") String resultImageKey,
            @JsonProperty("llm_title") String llmTitle,
            @JsonProperty("llm_comment") String llmComment,
            @JsonProperty("error_code") String errorCode,
            @JsonProperty("error_message") String errorMessage
    ) {
        this.fittingJobId = fittingJobId;
        this.status = status;
        this.resultImageKey = resultImageKey;
        this.llmTitle = llmTitle;
        this.llmComment = llmComment;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}
