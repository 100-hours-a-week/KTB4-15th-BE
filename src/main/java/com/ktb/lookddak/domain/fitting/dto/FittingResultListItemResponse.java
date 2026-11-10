package com.ktb.lookddak.domain.fitting.dto;

import com.ktb.lookddak.domain.fitting.entity.FittingResult;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Getter
public class FittingResultListItemResponse {

    private final Long fittingResultId;
    private final String resultImageUrl;
    private final String outfitName;
    private final Instant savedAt;

    private FittingResultListItemResponse(
            Long fittingResultId,
            String resultImageUrl,
            String outfitName,
            LocalDateTime savedAt
    ) {
        this.fittingResultId = fittingResultId;
        this.resultImageUrl = resultImageUrl;
        this.outfitName = outfitName;
        this.savedAt = savedAt == null ? null : savedAt.toInstant(ZoneOffset.UTC);
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
