package com.ktb.lookddak.domain.fitting.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.ktb.lookddak.domain.fitting.entity.FittingJob;
import com.ktb.lookddak.domain.fitting.entity.FittingJobStatus;
import lombok.Getter;

@Getter
@JsonPropertyOrder({"fittingJobId", "status", "result"})
public class FittingJobStatusResponse {

    private final Long fittingJobId;
    private final FittingJobStatus status;
    private final FittingResultResponse result;

    private FittingJobStatusResponse(
            Long fittingJobId,
            FittingJobStatus status,
            FittingResultResponse result
    ) {
        this.fittingJobId = fittingJobId;
        this.status = status;
        this.result = result;
    }

    public static FittingJobStatusResponse from(
            FittingJob fittingJob,
            FittingResultResponse result
    ) {
        return new FittingJobStatusResponse(
                fittingJob.getId(),
                fittingJob.getStatus(),
                result
        );
    }
}
