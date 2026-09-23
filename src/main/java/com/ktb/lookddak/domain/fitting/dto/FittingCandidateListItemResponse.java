package com.ktb.lookddak.domain.fitting.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.ktb.lookddak.domain.fitting.entity.FittingCandidate;
import com.ktb.lookddak.domain.product.entity.Product;
import com.ktb.lookddak.domain.product.entity.ProductItemType;
import lombok.Getter;

@Getter
@JsonPropertyOrder({
        "fittingCandidateId",
        "productId",
        "productName",
        "productImageUrl",
        "currentPrice",
        "color",
        "itemType"
})
public class FittingCandidateListItemResponse {

    private final Long fittingCandidateId;
    private final Long productId;
    private final String productName;
    private final String productImageUrl;
    private final Integer currentPrice;
    private final String color;
    private final ProductItemType itemType;

    private FittingCandidateListItemResponse(
            Long fittingCandidateId,
            Long productId,
            String productName,
            String productImageUrl,
            Integer currentPrice,
            String color,
            ProductItemType itemType
    ) {
        this.fittingCandidateId = fittingCandidateId;
        this.productId = productId;
        this.productName = productName;
        this.productImageUrl = productImageUrl;
        this.currentPrice = currentPrice;
        this.color = color;
        this.itemType = itemType;
    }

    public static FittingCandidateListItemResponse from(
            FittingCandidate candidate
    ) {
        Product product = candidate.getProduct();

        return new FittingCandidateListItemResponse(
                candidate.getId(),
                product.getId(),
                product.getName(),
                product.getImageUrl(),
                product.getCurrentPrice(),
                product.getColor(),
                product.getItemType()
        );
    }
}
