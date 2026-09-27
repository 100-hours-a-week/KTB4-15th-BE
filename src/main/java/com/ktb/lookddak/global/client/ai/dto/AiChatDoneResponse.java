package com.ktb.lookddak.global.client.ai.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class AiChatDoneResponse {

    private final Long chatId;
    private final String content;
    private final List<AiRecommendedProductResponse> products;

    @JsonCreator
    public AiChatDoneResponse(
            @JsonProperty("chat_id") Long chatId,
            @JsonProperty("content") String content,
            @JsonProperty("products")
            List<AiRecommendedProductResponse> products
    ) {
        this.chatId = chatId;
        this.content = content;
        this.products = products == null
                ? List.of()
                : List.copyOf(products);
    }
}
