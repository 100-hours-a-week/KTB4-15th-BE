package com.ktb.lookddak.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.ktb.lookddak.domain.chat.entity.ChatGenerationStatus;
import com.ktb.lookddak.domain.chat.entity.ChatMessage;
import com.ktb.lookddak.domain.chat.entity.ChatSenderType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@JsonPropertyOrder({
        "messageId",
        "senderType",
        "content",
        "generationStatus",
        "recommendation",
        "createdAt"
})
public class ChatMessageDetailResponse {

    private final Long messageId;
    private final ChatSenderType senderType;
    private final String content;
    private final ChatGenerationStatus generationStatus;
    private final RecommendationResponse recommendation;
    private final LocalDateTime createdAt;

    private ChatMessageDetailResponse(
            Long messageId,
            ChatSenderType senderType,
            String content,
            ChatGenerationStatus generationStatus,
            RecommendationResponse recommendation,
            LocalDateTime createdAt
    ) {
        this.messageId = messageId;
        this.senderType = senderType;
        this.content = content;
        this.generationStatus = generationStatus;
        this.recommendation = recommendation;
        this.createdAt = createdAt;
    }

    public static ChatMessageDetailResponse from(
            ChatMessage message,
            RecommendationResponse recommendation
    ) {
        return new ChatMessageDetailResponse(
                message.getId(),
                message.getSenderType(),
                message.getContent(),
                message.getGenerationStatus(),
                recommendation,
                message.getCreatedAt()
        );
    }
}
