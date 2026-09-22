package com.ktb.lookddak.domain.chat.dto;

import com.ktb.lookddak.domain.chat.entity.ChatRoom;
import lombok.Getter;

import java.util.List;

@Getter
public class ChatRoomDetailResponse {

    private final Long chatRoomId;
    private final String title;
    private final List<ChatMessageDetailResponse> messages;
    private final Long nextCursor;
    private final boolean hasNext;

    private ChatRoomDetailResponse(
            Long chatRoomId,
            String title,
            List<ChatMessageDetailResponse> messages,
            Long nextCursor,
            boolean hasNext
    ) {
        this.chatRoomId = chatRoomId;
        this.title = title;
        this.messages = List.copyOf(messages);
        this.nextCursor = nextCursor;
        this.hasNext = hasNext;
    }

    public static ChatRoomDetailResponse from(
            ChatRoom chatRoom,
            List<ChatMessageDetailResponse> messages,
            Long nextCursor,
            boolean hasNext
    ) {
        return new ChatRoomDetailResponse(
                chatRoom.getId(),
                chatRoom.getTitle(),
                messages,
                nextCursor,
                hasNext
        );
    }
}
