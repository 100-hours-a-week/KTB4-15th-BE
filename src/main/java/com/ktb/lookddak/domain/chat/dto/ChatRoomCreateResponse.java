package com.ktb.lookddak.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@JsonPropertyOrder({"chatRoomId", "messageId", "createdAt"})
public class ChatRoomCreateResponse {

    private final Long chatRoomId;
    private final Long messageId;
    private final LocalDateTime createdAt;

    public ChatRoomCreateResponse(
            Long chatRoomId,
            Long messageId,
            LocalDateTime createdAt
    ) {
        this.chatRoomId = chatRoomId;
        this.messageId = messageId;
        this.createdAt = createdAt;
    }
}
