package com.ktb.lookddak.domain.chat.dto;

import com.ktb.lookddak.domain.chat.entity.ChatMessage;
import lombok.Getter;

@Getter
public class ChatRoomSearchItemResponse {

    private final Long chatRoomId;
    private final String title;
    private final Long messageId;
    private final String message;

    private ChatRoomSearchItemResponse(
            Long chatRoomId,
            String title,
            Long messageId,
            String message
    ) {
        this.chatRoomId = chatRoomId;
        this.title = title;
        this.messageId = messageId;
        this.message = message;
    }

    public static ChatRoomSearchItemResponse from(ChatMessage chatMessage) {
        return new ChatRoomSearchItemResponse(
                chatMessage.getChatRoom().getId(),
                chatMessage.getChatRoom().getTitle(),
                chatMessage.getId(),
                chatMessage.getContent()
        );
    }
}
