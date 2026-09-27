package com.ktb.lookddak.domain.fitting.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.ktb.lookddak.domain.fitting.entity.FittingJob;
import com.ktb.lookddak.domain.fitting.entity.FittingJobStatus;
import lombok.Getter;

@Getter
@JsonPropertyOrder({"fittingJobId", "status"})
public class FittingJobCreateResponse {

    private final Long fittingJobId;
    private final FittingJobStatus status;

    private FittingJobCreateResponse(
            Long fittingJobId,
            FittingJobStatus status
    ) {
        this.fittingJobId = fittingJobId;
        this.status = status;
    }

    public static FittingJobCreateResponse from(FittingJob fittingJob) {
        return new FittingJobCreateResponse(
                fittingJob.getId(),
                fittingJob.getStatus()
        );
    }
}
