package com.ktb.lookddak.domain.chat.dto;

import com.ktb.lookddak.global.client.ai.dto.AiRecommendedProductResponse;
import lombok.Getter;

@Getter
public class ChatSseProductResponse {

    private final Long userMessageId;
    private final AiRecommendedProductResponse product;

    public ChatSseProductResponse(
            Long userMessageId,
            AiRecommendedProductResponse product
    ) {
        this.userMessageId = userMessageId;
        this.product = product;
    }
}
