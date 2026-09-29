package com.ktb.lookddak.domain.fitting.event;

import com.ktb.lookddak.domain.fitting.service.FittingGenerationResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class FittingGenerationResultEventHandler {

    private final FittingGenerationResultService resultService;

    @EventListener
    public void handleSucceeded(FittingGenerationSucceededEvent event) {
        resultService.complete(event.getFittingJobId(), event.getResult());
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleFailed(FittingGenerationFailedEvent event) {
        resultService.fail(event.getFittingJobId());
    }
}
