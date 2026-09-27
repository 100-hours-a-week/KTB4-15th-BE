package com.ktb.lookddak.global.client.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.ktb.lookddak.domain.chat.entity.ChatSourceType;
import lombok.Getter;

import java.util.List;

@Getter
@JsonPropertyOrder({
        "chat_id",
        "user_id",
        "message",
        "source_type",
        "product_ids"
})
public class AiChatRequest {

    @JsonProperty("chat_id")
    private final Long chatId;

    @JsonProperty("user_id")
    private final Long userId;

    private final String message;

    @JsonProperty("source_type")
    private final ChatSourceType sourceType;

    @JsonProperty("product_ids")
    private final List<String> productIds;

    public AiChatRequest(
            Long chatId,
            Long userId,
            String message,
            ChatSourceType sourceType,
            List<String> productIds
    ) {
        this.chatId = chatId;
        this.userId = userId;
        this.message = message;
        this.sourceType = sourceType;
        this.productIds = productIds == null
                ? List.of()
                : List.copyOf(productIds);
    }
}
