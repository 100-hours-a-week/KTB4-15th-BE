package com.ktb.lookddak.global.client.ai.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ktb.lookddak.domain.product.entity.ProductItemType;
import lombok.Getter;

@Getter
public class AiRecommendedProductResponse {

    private final String productCode;
    private final String productName;
    private final String productImageUrl;
    private final String purchaseUrl;
    private final String color;
    private final ProductItemType itemType;
    private final Integer currentPrice;
    private final String recommendedReason;

    @JsonCreator
    public AiRecommendedProductResponse(
            @JsonProperty("product_code") String productCode,
            @JsonProperty("product_name") String productName,
            @JsonProperty("product_image_url") String productImageUrl,
            @JsonProperty("purchase_url") String purchaseUrl,
            @JsonProperty("color") String color,
            @JsonProperty("item_type") ProductItemType itemType,
            @JsonProperty("current_price") Integer currentPrice,
            @JsonProperty("recommended_reason") String recommendedReason
    ) {
        this.productCode = productCode;
        this.productName = productName;
        this.productImageUrl = productImageUrl;
        this.purchaseUrl = purchaseUrl;
        this.color = color;
        this.itemType = itemType;
        this.currentPrice = currentPrice;
        this.recommendedReason = recommendedReason;
    }
}
