package com.ktb.lookddak.domain.fitting.event;

import lombok.Getter;

@Getter
public class FittingGenerationRequestedEvent {

    private final Long fittingJobId;

    public FittingGenerationRequestedEvent(Long fittingJobId) {
        this.fittingJobId = fittingJobId;
    }
}
