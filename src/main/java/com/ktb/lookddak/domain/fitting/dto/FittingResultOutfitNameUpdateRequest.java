package com.ktb.lookddak.domain.fitting.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FittingResultOutfitNameUpdateRequest {

    private String outfitName;

    public FittingResultOutfitNameUpdateRequest(String outfitName) {
        this.outfitName = outfitName;
    }
}
