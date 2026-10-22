package com.ktb.lookddak.domain.chat.dto;

import lombok.Getter;

@Getter
public class ChatSseConnectedResponse {

    private final Long userMessageId;

    public ChatSseConnectedResponse(Long userMessageId) {
        this.userMessageId = userMessageId;
    }
}
