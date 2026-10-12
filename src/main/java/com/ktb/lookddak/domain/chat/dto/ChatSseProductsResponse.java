package com.ktb.lookddak.domain.chat.dto;

import com.ktb.lookddak.global.client.ai.dto.AiRecommendedProductResponse;
import lombok.Getter;

import java.util.List;

@Getter
public class ChatSseProductsResponse {

    private final Long userMessageId;
    private final List<AiRecommendedProductResponse> products;

    public ChatSseProductsResponse(
            Long userMessageId,
            List<AiRecommendedProductResponse> products
    ) {
        this.userMessageId = userMessageId;
        this.products = products == null ? List.of() : List.copyOf(products);
    }
}
