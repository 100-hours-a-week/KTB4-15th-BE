package com.ktb.lookddak.domain.chat.dto;

import lombok.Getter;

@Getter
public class ChatSseSnapshotResponse {

    private final Long userMessageId;
    private final String content;

    public ChatSseSnapshotResponse(Long userMessageId, String content) {
        this.userMessageId = userMessageId;
        this.content = content;
    }
}
