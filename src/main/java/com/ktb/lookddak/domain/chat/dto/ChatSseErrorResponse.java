package com.ktb.lookddak.domain.chat.dto;

import lombok.Getter;

@Getter
public class ChatSseErrorResponse {

    private final Long userMessageId;
    private final String code;
    private final String message;

    public ChatSseErrorResponse(
            Long userMessageId,
            String code,
            String message
    ) {
        this.userMessageId = userMessageId;
        this.code = code;
        this.message = message;
    }
}
