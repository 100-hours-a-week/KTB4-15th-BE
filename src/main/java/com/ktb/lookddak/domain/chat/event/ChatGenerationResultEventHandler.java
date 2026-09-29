package com.ktb.lookddak.domain.chat.event;

import com.ktb.lookddak.domain.chat.service.ChatGenerationResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ChatGenerationResultEventHandler {

    private final ChatGenerationResultService resultService;

    @EventListener
    public void handleSucceeded(ChatGenerationSucceededEvent event) {
        resultService.complete(
                event.getUserMessageId(),
                event.getResponse()
        );
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleFailed(ChatGenerationFailedEvent event) {
        resultService.fail(event.getUserMessageId());
    }
}
