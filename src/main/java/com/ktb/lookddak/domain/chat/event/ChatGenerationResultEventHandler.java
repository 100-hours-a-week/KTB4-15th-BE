package com.ktb.lookddak.domain.chat.event;

import com.ktb.lookddak.domain.chat.service.ChatGenerationResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

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
    public void handleFailed(ChatGenerationFailedEvent event) {
        resultService.fail(event.getUserMessageId());
    }
}
