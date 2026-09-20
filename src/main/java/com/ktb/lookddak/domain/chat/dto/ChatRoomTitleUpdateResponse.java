package com.ktb.lookddak.domain.chat.dto;

import lombok.Getter;

@Getter
public class ChatRoomTitleUpdateResponse {

    private final Long chatRoomId;
    private final String title;

    public ChatRoomTitleUpdateResponse(Long chatRoomId, String title) {
        this.chatRoomId = chatRoomId;
        this.title = title;
    }
}
