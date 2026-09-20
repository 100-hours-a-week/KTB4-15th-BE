package com.ktb.lookddak.domain.chat.dto;

import lombok.Getter;

@Getter
public class ChatRoomCreateResponse {

    private final Long chatRoomId;
    private final Long messageId;

    public ChatRoomCreateResponse(Long chatRoomId, Long messageId) {
        this.chatRoomId = chatRoomId;
        this.messageId = messageId;
    }
}
