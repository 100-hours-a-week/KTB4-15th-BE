package com.ktb.lookddak.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@JsonPropertyOrder({"chatRoomId", "messageId", "content", "createdAt"})
public class ChatMessageCreateResponse {

    private final Long chatRoomId;
    private final Long messageId;
    private final String content;
    private final LocalDateTime createdAt;

    public ChatMessageCreateResponse(
            Long chatRoomId,
            Long messageId,
            String content,
            LocalDateTime createdAt
    ) {
        this.chatRoomId = chatRoomId;
        this.messageId = messageId;
        this.content = content;
        this.createdAt = createdAt;
    }
}
