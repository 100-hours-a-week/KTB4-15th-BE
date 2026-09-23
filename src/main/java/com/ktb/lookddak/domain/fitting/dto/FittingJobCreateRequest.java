package com.ktb.lookddak.domain.fitting.dto;

import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FittingJobCreateRequest {

    @Positive
    private Long topProductId;

    @Positive
    private Long bottomProductId;

    public FittingJobCreateRequest(
            Long topProductId,
            Long bottomProductId
    ) {
        this.topProductId = topProductId;
        this.bottomProductId = bottomProductId;
    }
}
