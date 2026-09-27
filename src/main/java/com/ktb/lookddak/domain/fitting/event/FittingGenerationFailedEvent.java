package com.ktb.lookddak.domain.fitting.event;

import lombok.Getter;

@Getter
public class FittingGenerationFailedEvent {

    private final Long fittingJobId;
    private final String code;
    private final String message;

    public FittingGenerationFailedEvent(
            Long fittingJobId,
            String code,
            String message
    ) {
        this.fittingJobId = fittingJobId;
        this.code = code;
        this.message = message;
    }
}
