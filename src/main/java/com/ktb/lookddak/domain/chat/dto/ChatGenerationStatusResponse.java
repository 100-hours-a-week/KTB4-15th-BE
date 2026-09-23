package com.ktb.lookddak.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.ktb.lookddak.domain.chat.entity.ChatGenerationStatus;
import lombok.Getter;

@Getter
@JsonPropertyOrder({"generationStatus", "message"})
public class ChatGenerationStatusResponse {

    private final ChatGenerationStatus generationStatus;
    private final ChatMessageDetailResponse message;

    public ChatGenerationStatusResponse(
            ChatGenerationStatus generationStatus,
            ChatMessageDetailResponse message
    ) {
        this.generationStatus = generationStatus;
        this.message = message;
    }
}
