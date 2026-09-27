package com.ktb.lookddak.global.client.ai.exception;

import lombok.Getter;

@Getter
public class AiChatException extends RuntimeException {

    private final String code;
    private final Long chatId;

    public AiChatException(String code, String message) {
        this(code, message, null, null);
    }

    public AiChatException(String code, String message, Long chatId) {
        this(code, message, chatId, null);
    }

    public AiChatException(
            String code,
            String message,
            Long chatId,
            Throwable cause
    ) {
        super(message, cause);
        this.code = code;
        this.chatId = chatId;
    }
}
