package com.ktb.lookddak.domain.chat.dto;

import com.ktb.lookddak.global.client.ai.dto.AiRecommendedProductResponse;
import lombok.Getter;

import java.util.List;

@Getter
public class ChatSseDoneResponse {

    private final Long userMessageId;
    private final String content;
    private final List<AiRecommendedProductResponse> products;

    public ChatSseDoneResponse(
            Long userMessageId,
            String content,
            List<AiRecommendedProductResponse> products
    ) {
        this.userMessageId = userMessageId;
        this.content = content;
        this.products = products == null ? List.of() : List.copyOf(products);
    }
}
