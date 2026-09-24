package com.ktb.lookddak.domain.fitting.dto;

import lombok.Getter;

@Getter
public class FittingCandidateBulkDeleteResponse {

    private final int deletedCount;

    public FittingCandidateBulkDeleteResponse(int deletedCount) {
        this.deletedCount = deletedCount;
    }
}
