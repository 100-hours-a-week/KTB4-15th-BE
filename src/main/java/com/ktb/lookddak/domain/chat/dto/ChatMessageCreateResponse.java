package com.ktb.lookddak.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Getter
@JsonPropertyOrder({"chatRoomId", "messageId", "content", "createdAt"})
public class ChatMessageCreateResponse {

    private final Long chatRoomId;
    private final Long messageId;
    private final String content;
    private final Instant createdAt;

    public ChatMessageCreateResponse(
            Long chatRoomId,
            Long messageId,
            String content,
            LocalDateTime createdAt
    ) {
        this.chatRoomId = chatRoomId;
        this.messageId = messageId;
        this.content = content;
        this.createdAt = createdAt == null ? null : createdAt.toInstant(ZoneOffset.UTC);
    }
}
