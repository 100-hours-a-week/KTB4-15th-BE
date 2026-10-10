package com.ktb.lookddak.domain.fitting.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.ktb.lookddak.domain.fitting.entity.FittingResult;
import lombok.Getter;

@Getter
@JsonPropertyOrder({"fittingResultId", "outfitName"})
public class FittingResultOutfitNameUpdateResponse {

    private final Long fittingResultId;
    private final String outfitName;

    private FittingResultOutfitNameUpdateResponse(
            Long fittingResultId,
            String outfitName
    ) {
        this.fittingResultId = fittingResultId;
        this.outfitName = outfitName;
    }

    public static FittingResultOutfitNameUpdateResponse from(
            FittingResult fittingResult
    ) {
        return new FittingResultOutfitNameUpdateResponse(
                fittingResult.getId(),
                fittingResult.getOutfitName()
        );
    }
}
