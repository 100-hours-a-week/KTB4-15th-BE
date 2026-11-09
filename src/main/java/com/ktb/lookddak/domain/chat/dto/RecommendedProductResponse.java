package com.ktb.lookddak.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
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
        "wishlistId",
        "isFittingCandidate",
        "fittingCandidateId"
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

    private final Long wishlistId;

    @JsonProperty("isFittingCandidate")
    private final boolean fittingCandidate;

    private final Long fittingCandidateId;

    @JsonCreator
    private RecommendedProductResponse(
            @JsonProperty("productId") Long productId,
            @JsonProperty("productName") String productName,
            @JsonProperty("productImageUrl") String productImageUrl,
            @JsonProperty("currentPrice") Integer currentPrice,
            @JsonProperty("color") String color,
            @JsonProperty("itemType") ProductItemType itemType,
            @JsonProperty("purchaseUrl") String purchaseUrl,
            @JsonProperty("recommendedReason") String recommendedReason,
            @JsonProperty("isWishlisted") boolean wishlisted,
            @JsonProperty("wishlistId") Long wishlistId,
            @JsonProperty("isFittingCandidate") boolean fittingCandidate,
            @JsonProperty("fittingCandidateId") Long fittingCandidateId
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
        this.wishlistId = wishlistId;
        this.fittingCandidate = fittingCandidate;
        this.fittingCandidateId = fittingCandidateId;
    }

    public static RecommendedProductResponse from(
            RecommendationProduct recommendationProduct,
            Long wishlistId,
            Long fittingCandidateId
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
                wishlistId != null,
                wishlistId,
                fittingCandidateId != null,
                fittingCandidateId
        );
    }
}
