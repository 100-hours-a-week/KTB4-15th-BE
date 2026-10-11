package com.ktb.lookddak.domain.product.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.ktb.lookddak.domain.product.entity.Product;
import com.ktb.lookddak.domain.product.entity.ProductItemType;
import lombok.Getter;

@Getter
@JsonPropertyOrder({
        "rank",
        "productId",
        "productName",
        "productImageUrl",
        "purchaseUrl",
        "currentPrice",
        "color",
        "itemType",
        "rankingCount"
})
public class ProductRankingItemResponse {

    private final int rank;
    private final Long productId;
    private final String productName;
    private final String productImageUrl;
    private final String purchaseUrl;
    private final Integer currentPrice;
    private final String color;
    private final ProductItemType itemType;
    private final Long rankingCount;

    private ProductRankingItemResponse(
            int rank,
            Product product,
            Long rankingCount
    ) {
        this.rank = rank;
        this.productId = product.getId();
        this.productName = product.getName();
        this.productImageUrl = product.getImageUrl();
        this.purchaseUrl = product.getPurchaseUrl();
        this.currentPrice = product.getCurrentPrice();
        this.color = product.getColor();
        this.itemType = product.getItemType();
        this.rankingCount = rankingCount;
    }

    public static ProductRankingItemResponse from(
            int rank,
            ProductRankingQueryResult result
    ) {
        return new ProductRankingItemResponse(
                rank,
                result.product(),
                result.rankingCount()
        );
    }
}
