package com.ktb.lookddak.domain.fitting.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.ktb.lookddak.domain.fitting.entity.FittingJob;
import lombok.Getter;

@Getter
@JsonPropertyOrder({"fittingJobId"})
public class FittingJobResultViewedResponse {

    private final Long fittingJobId;

    private FittingJobResultViewedResponse(Long fittingJobId) {
        this.fittingJobId = fittingJobId;
    }

    public static FittingJobResultViewedResponse from(FittingJob fittingJob) {
        return new FittingJobResultViewedResponse(fittingJob.getId());
    }
}
