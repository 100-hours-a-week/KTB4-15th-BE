package com.ktb.lookddak.domain.fitting.event;

import com.ktb.lookddak.global.client.ai.fitting.dto.AiFittingResultData;
import lombok.Getter;

@Getter
public class FittingGenerationSucceededEvent {

    private final Long fittingJobId;
    private final AiFittingResultData result;

    public FittingGenerationSucceededEvent(
            Long fittingJobId,
            AiFittingResultData result
    ) {
        this.fittingJobId = fittingJobId;
        this.result = result;
    }
}
