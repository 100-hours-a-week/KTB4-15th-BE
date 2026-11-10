package com.ktb.lookddak.domain.fitting.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.ktb.lookddak.domain.fitting.entity.FittingJob;
import com.ktb.lookddak.domain.fitting.entity.FittingJobStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@JsonPropertyOrder({"fittingJobId", "status", "createdAt", "result"})
public class FittingJobStatusResponse {

    private final Long fittingJobId;
    private final FittingJobStatus status;
    private final LocalDateTime createdAt;
    private final FittingResultResponse result;

    private FittingJobStatusResponse(
            Long fittingJobId,
            FittingJobStatus status,
            LocalDateTime createdAt,
            FittingResultResponse result
    ) {
        this.fittingJobId = fittingJobId;
        this.status = status;
        this.createdAt = createdAt;
        this.result = result;
    }

    public static FittingJobStatusResponse from(
            FittingJob fittingJob,
            FittingResultResponse result
    ) {
        return new FittingJobStatusResponse(
                fittingJob.getId(),
                fittingJob.getStatus(),
                fittingJob.getCreatedAt(),
                result
        );
    }
}
