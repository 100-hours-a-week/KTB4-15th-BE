package com.ktb.lookddak.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.ktb.lookddak.domain.product.entity.Product;
import com.ktb.lookddak.domain.product.entity.ProductItemType;
import com.ktb.lookddak.domain.recommendation.entity.RecommendationProduct;
import lombok.Getter;

@Getter
@JsonPropertyOrder({
        "productId",
        "productName",
        "productImageUrl",
        "currentPrice",
        "color",
        "itemType",
        "purchaseUrl",
        "recommendedReason",
        "isWishlisted",
        "isFittingCandidate"
})
public class RecommendedProductResponse {

    private final Long productId;
    private final String productName;
    private final String productImageUrl;
    private final Integer currentPrice;
    private final String color;
    private final ProductItemType itemType;
    private final String purchaseUrl;
    private final String recommendedReason;

    @JsonProperty("isWishlisted")
    private final boolean wishlisted;

    @JsonProperty("isFittingCandidate")
    private final boolean fittingCandidate;

    private RecommendedProductResponse(
            Long productId,
            String productName,
            String productImageUrl,
            Integer currentPrice,
            String color,
            ProductItemType itemType,
            String purchaseUrl,
            String recommendedReason,
            boolean wishlisted,
            boolean fittingCandidate
    ) {
        this.productId = productId;
        this.productName = productName;
        this.productImageUrl = productImageUrl;
        this.currentPrice = currentPrice;
        this.color = color;
        this.itemType = itemType;
        this.purchaseUrl = purchaseUrl;
        this.recommendedReason = recommendedReason;
        this.wishlisted = wishlisted;
        this.fittingCandidate = fittingCandidate;
    }

    public static RecommendedProductResponse from(
            RecommendationProduct recommendationProduct,
            boolean wishlisted,
            boolean fittingCandidate
    ) {
        Product product = recommendationProduct.getProduct();

        return new RecommendedProductResponse(
                product.getId(),
                product.getName(),
                product.getImageUrl(),
                recommendationProduct.getPriceSnapshot(),
                product.getColor(),
                product.getItemType(),
                product.getPurchaseUrl(),
                recommendationProduct.getRecommendedReason(),
                wishlisted,
                fittingCandidate
        );
    }
}
