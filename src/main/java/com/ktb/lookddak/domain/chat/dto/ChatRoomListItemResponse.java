package com.ktb.lookddak.domain.chat.dto;

import com.ktb.lookddak.domain.chat.entity.ChatRoom;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Getter
public class ChatRoomListItemResponse {

    private final Long chatRoomId;
    private final String title;
    private final Instant lastMessageAt;

    private ChatRoomListItemResponse(
            Long chatRoomId,
            String title,
            LocalDateTime lastMessageAt
    ) {
        this.chatRoomId = chatRoomId;
        this.title = title;
        this.lastMessageAt = lastMessageAt == null ? null : lastMessageAt.toInstant(ZoneOffset.UTC);
    }

    public static ChatRoomListItemResponse from(ChatRoom chatRoom) {
        return new ChatRoomListItemResponse(
                chatRoom.getId(),
                chatRoom.getTitle(),
                chatRoom.getLastMessageAt()
        );
    }
}
