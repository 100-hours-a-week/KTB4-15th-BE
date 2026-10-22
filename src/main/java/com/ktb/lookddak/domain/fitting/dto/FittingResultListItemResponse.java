package com.ktb.lookddak.domain.fitting.dto;

import com.ktb.lookddak.domain.fitting.entity.FittingResult;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class FittingResultListItemResponse {

    private final Long fittingResultId;
    private final String resultImageUrl;
    private final String outfitName;
    private final LocalDateTime savedAt;

    private FittingResultListItemResponse(
            Long fittingResultId,
            String resultImageUrl,
            String outfitName,
            LocalDateTime savedAt
    ) {
        this.fittingResultId = fittingResultId;
        this.resultImageUrl = resultImageUrl;
        this.outfitName = outfitName;
        this.savedAt = savedAt;
    }

    public static FittingResultListItemResponse from(
            FittingResult fittingResult,
            String resultImageUrl
    ) {
        return new FittingResultListItemResponse(
                fittingResult.getId(),
                resultImageUrl,
                fittingResult.getOutfitName(),
                fittingResult.getCreatedAt()
        );
    }
}
