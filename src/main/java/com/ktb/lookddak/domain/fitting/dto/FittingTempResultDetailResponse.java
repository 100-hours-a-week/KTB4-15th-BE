package com.ktb.lookddak.domain.fitting.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.ktb.lookddak.domain.fitting.entity.FittingJob;
import com.ktb.lookddak.domain.fitting.entity.FittingResult;
import com.ktb.lookddak.domain.fitting.entity.FittingTempResult;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@JsonPropertyOrder({
        "fittingJobId",
        "resultViewed",
        "saved",
        "fittingResultId",
        "resultImageUrl",
        "outfitName",
        "comment",
        "products",
        "expiresAt"
})
public class FittingTempResultDetailResponse {

    private final Long fittingJobId;
    private final boolean resultViewed;
    private final boolean saved;
    private final Long fittingResultId;
    private final String resultImageUrl;
    private final String outfitName;
    private final String comment;
    private final List<FittingResultProductResponse> products;
    private final LocalDateTime expiresAt;

    private FittingTempResultDetailResponse(
            Long fittingJobId,
            boolean resultViewed,
            boolean saved,
            Long fittingResultId,
            String resultImageUrl,
            String outfitName,
            String comment,
            List<FittingResultProductResponse> products,
            LocalDateTime expiresAt
    ) {
        this.fittingJobId = fittingJobId;
        this.resultViewed = resultViewed;
        this.saved = saved;
        this.fittingResultId = fittingResultId;
        this.resultImageUrl = resultImageUrl;
        this.outfitName = outfitName;
        this.comment = comment;
        this.products = List.copyOf(products);
        this.expiresAt = expiresAt;
    }

    public static FittingTempResultDetailResponse from(
            FittingJob fittingJob,
            FittingTempResult tempResult,
            FittingResult fittingResult,
            String resultImageUrl,
            List<FittingResultProductResponse> products
    ) {
        boolean saved = fittingResult != null && !fittingResult.isDeleted();
        return new FittingTempResultDetailResponse(
                fittingJob.getId(),
                fittingJob.isResultViewed(),
                saved,
                saved ? fittingResult.getId() : null,
                resultImageUrl,
                tempResult.getOutfitName(),
                tempResult.getAiComment(),
                products,
                tempResult.getExpiresAt()
        );
    }
}
