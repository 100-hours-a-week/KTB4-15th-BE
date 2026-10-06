package com.ktb.lookddak.global.client.ai.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class AiChatProductResponse {

    private final Long chatId;
    private final AiRecommendedProductResponse product;

    @JsonCreator
    public AiChatProductResponse(
            @JsonProperty("chat_id") Long chatId,
            @JsonProperty("product") AiRecommendedProductResponse product
    ) {
        this.chatId = chatId;
        this.product = product;
    }
}
