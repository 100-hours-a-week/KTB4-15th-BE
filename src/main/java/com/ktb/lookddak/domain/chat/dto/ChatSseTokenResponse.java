package com.ktb.lookddak.domain.chat.dto;

import lombok.Getter;

@Getter
public class ChatSseTokenResponse {

    private final Long userMessageId;
    private final String content;

    public ChatSseTokenResponse(Long userMessageId, String content) {
        this.userMessageId = userMessageId;
        this.content = content;
    }
}
