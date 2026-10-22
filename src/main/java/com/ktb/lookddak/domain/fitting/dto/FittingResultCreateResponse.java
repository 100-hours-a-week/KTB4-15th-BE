package com.ktb.lookddak.domain.fitting.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.ktb.lookddak.domain.fitting.entity.FittingResult;
import lombok.Getter;

@Getter
@JsonPropertyOrder({"fittingResultId", "outfitName"})
public class FittingResultCreateResponse {

    private final Long fittingResultId;
    private final String outfitName;

    private FittingResultCreateResponse(Long fittingResultId, String outfitName) {
        this.fittingResultId = fittingResultId;
        this.outfitName = outfitName;
    }

    public static FittingResultCreateResponse from(FittingResult fittingResult) {
        return new FittingResultCreateResponse(
                fittingResult.getId(),
                fittingResult.getOutfitName()
        );
    }
}
