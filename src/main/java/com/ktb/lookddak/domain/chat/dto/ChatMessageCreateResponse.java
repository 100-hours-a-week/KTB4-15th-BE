package com.ktb.lookddak.domain.chat.dto;

import lombok.Getter;

@Getter
public class ChatMessageCreateResponse {

    private final Long chatRoomId;
    private final Long messageId;
    private final String content;

    public ChatMessageCreateResponse(
            Long chatRoomId,
            Long messageId,
            String content
    ) {
        this.chatRoomId = chatRoomId;
        this.messageId = messageId;
        this.content = content;
    }
}
