package com.ktb.lookddak.domain.chat.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class ChatSseDoneResponse {

    private final Long userMessageId;
    private final String content;
    private final List<RecommendedProductResponse> products;

    public ChatSseDoneResponse(
            Long userMessageId,
            String content,
            List<RecommendedProductResponse> products
    ) {
        this.userMessageId = userMessageId;
        this.content = content;
        this.products = products == null ? List.of() : List.copyOf(products);
    }
}
