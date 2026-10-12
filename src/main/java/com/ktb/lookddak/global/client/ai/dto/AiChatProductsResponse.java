package com.ktb.lookddak.global.client.ai.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class AiChatProductsResponse {

    private final Long chatId;
    private final List<AiRecommendedProductResponse> products;

    @JsonCreator
    public AiChatProductsResponse(
            @JsonProperty("chat_id") Long chatId,
            @JsonProperty("products") List<AiRecommendedProductResponse> products
    ) {
        this.chatId = chatId;
        this.products = products == null ? List.of() : List.copyOf(products);
    }
}
