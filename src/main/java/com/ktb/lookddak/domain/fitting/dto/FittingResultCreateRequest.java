package com.ktb.lookddak.domain.fitting.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FittingResultCreateRequest {

    @NotNull
    @Positive
    private Long fittingJobId;

    private String outfitName;

    public FittingResultCreateRequest(Long fittingJobId, String outfitName) {
        this.fittingJobId = fittingJobId;
        this.outfitName = outfitName;
    }
}
