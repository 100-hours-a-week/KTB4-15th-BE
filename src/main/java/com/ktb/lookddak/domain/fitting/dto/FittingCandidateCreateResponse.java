package com.ktb.lookddak.domain.fitting.dto;

import lombok.Getter;

@Getter
public class FittingCandidateCreateResponse {

    private final Long fittingCandidateId;
    private final Long productId;

    public FittingCandidateCreateResponse(
            Long fittingCandidateId,
            Long productId
    ) {
        this.fittingCandidateId = fittingCandidateId;
        this.productId = productId;
    }
}
