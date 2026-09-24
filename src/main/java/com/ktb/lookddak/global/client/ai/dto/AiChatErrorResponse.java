package com.ktb.lookddak.global.client.ai.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class AiChatErrorResponse {

    private final Long chatId;
    private final String code;
    private final String message;

    @JsonCreator
    public AiChatErrorResponse(
            @JsonProperty("chat_id") Long chatId,
            @JsonProperty("code") String code,
            @JsonProperty("message") String message
    ) {
        this.chatId = chatId;
        this.code = code;
        this.message = message;
    }
}
