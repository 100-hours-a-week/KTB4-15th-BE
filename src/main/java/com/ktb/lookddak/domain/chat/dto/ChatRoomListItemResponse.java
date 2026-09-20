package com.ktb.lookddak.domain.chat.dto;

import com.ktb.lookddak.domain.chat.entity.ChatRoom;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ChatRoomListItemResponse {

    private final Long chatRoomId;
    private final String title;
    private final LocalDateTime lastMessageAt;

    private ChatRoomListItemResponse(
            Long chatRoomId,
            String title,
            LocalDateTime lastMessageAt
    ) {
        this.chatRoomId = chatRoomId;
        this.title = title;
        this.lastMessageAt = lastMessageAt;
    }

    public static ChatRoomListItemResponse from(ChatRoom chatRoom) {
        return new ChatRoomListItemResponse(
                chatRoom.getId(),
                chatRoom.getTitle(),
                chatRoom.getLastMessageAt()
        );
    }
}
