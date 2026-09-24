package com.ktb.lookddak.domain.chat.event;

import lombok.Getter;

@Getter
public class ChatGenerationFailedEvent {

    private final Long userMessageId;
    private final String code;
    private final String message;

    public ChatGenerationFailedEvent(
            Long userMessageId,
            String code,
            String message
    ) {
        this.userMessageId = userMessageId;
        this.code = code;
        this.message = message;
    }
}
