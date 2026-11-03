package com.ktb.lookddak.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.ktb.lookddak.domain.chat.entity.ChatRoom;
import lombok.Getter;

import java.util.List;

@Getter
@JsonPropertyOrder({
        "chatRoomId",
        "title",
        "anchorMessageId",
        "messages",
        "previousCursor",
        "nextCursor",
        "hasPrevious",
        "hasNext"
})
public class ChatMessageContextResponse {

    private final Long chatRoomId;
    private final String title;
    private final Long anchorMessageId;
    private final List<ChatMessageDetailResponse> messages;
    private final Long previousCursor;
    private final Long nextCursor;
    private final boolean hasPrevious;
    private final boolean hasNext;

    private ChatMessageContextResponse(
            Long chatRoomId,
            String title,
            Long anchorMessageId,
            List<ChatMessageDetailResponse> messages,
            Long previousCursor,
            Long nextCursor,
            boolean hasPrevious,
            boolean hasNext
    ) {
        this.chatRoomId = chatRoomId;
        this.title = title;
        this.anchorMessageId = anchorMessageId;
        this.messages = List.copyOf(messages);
        this.previousCursor = previousCursor;
        this.nextCursor = nextCursor;
        this.hasPrevious = hasPrevious;
        this.hasNext = hasNext;
    }

    public static ChatMessageContextResponse from(
            ChatRoom chatRoom,
            Long anchorMessageId,
            List<ChatMessageDetailResponse> messages,
            Long previousCursor,
            Long nextCursor,
            boolean hasPrevious,
            boolean hasNext
    ) {
        return new ChatMessageContextResponse(
                chatRoom.getId(),
                chatRoom.getTitle(),
                anchorMessageId,
                messages,
                previousCursor,
                nextCursor,
                hasPrevious,
                hasNext
        );
    }
}
