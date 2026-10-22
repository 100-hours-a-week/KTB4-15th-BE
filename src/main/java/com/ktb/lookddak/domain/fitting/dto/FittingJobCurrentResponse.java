package com.ktb.lookddak.domain.fitting.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.ktb.lookddak.domain.fitting.entity.FittingJob;
import com.ktb.lookddak.domain.fitting.entity.FittingJobStatus;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@JsonPropertyOrder({
        "fittingJobId",
        "status",
        "createdAt",
        "resultViewed",
        "products"
})
public class FittingJobCurrentResponse {

    private final Long fittingJobId;
    private final FittingJobStatus status;
    private final LocalDateTime createdAt;
    private final boolean resultViewed;
    private final List<FittingJobCurrentProductResponse> products;

    private FittingJobCurrentResponse(
            Long fittingJobId,
            FittingJobStatus status,
            LocalDateTime createdAt,
            boolean resultViewed,
            List<FittingJobCurrentProductResponse> products
    ) {
        this.fittingJobId = fittingJobId;
        this.status = status;
        this.createdAt = createdAt;
        this.resultViewed = resultViewed;
        this.products = products;
    }

    public static FittingJobCurrentResponse from(
            FittingJob fittingJob,
            List<FittingJobCurrentProductResponse> products
    ) {
        return new FittingJobCurrentResponse(
                fittingJob.getId(),
                fittingJob.getStatus(),
                fittingJob.getCreatedAt(),
                fittingJob.isResultViewed(),
                products
        );
    }
}
