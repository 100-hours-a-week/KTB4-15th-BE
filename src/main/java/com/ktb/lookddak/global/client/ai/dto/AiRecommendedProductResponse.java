package com.ktb.lookddak.global.client.ai.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ktb.lookddak.domain.product.entity.ProductItemType;
import lombok.Getter;

@Getter
public class AiRecommendedProductResponse {

    private final String productCode;
    private final String productName;
    private final String imageUrl;
    private final String detailUrl;
    private final String color;
    private final ProductItemType itemType;
    private final Integer price;
    private final String llmComment;

    @JsonCreator
    public AiRecommendedProductResponse(
            @JsonProperty("product_code") String productCode,
            @JsonProperty("product_name") String productName,
            @JsonProperty("image_url") String imageUrl,
            @JsonProperty("detail_url") String detailUrl,
            @JsonProperty("color") String color,
            @JsonProperty("item_type") ProductItemType itemType,
            @JsonProperty("price") Integer price,
            @JsonProperty("llm_comment") String llmComment
    ) {
        this.productCode = productCode;
        this.productName = productName;
        this.imageUrl = imageUrl;
        this.detailUrl = detailUrl;
        this.color = color;
        this.itemType = itemType;
        this.price = price;
        this.llmComment = llmComment;
    }
}
