package com.ktb.lookddak.domain.fitting.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FittingCandidateCreateRequest {

    @NotNull
    @Positive
    private Long productId;

    public FittingCandidateCreateRequest(Long productId) {
        this.productId = productId;
    }
}
