package com.ktb.lookddak.global.client.ai.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class AiChatTokenResponse {

    private final Long chatId;
    private final String content;

    @JsonCreator
    public AiChatTokenResponse(
            @JsonProperty("chat_id") Long chatId,
            @JsonProperty("content") String content
    ) {
        this.chatId = chatId;
        this.content = content;
    }
}
