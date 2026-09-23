package com.ktb.lookddak.domain.fitting.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.ktb.lookddak.domain.product.entity.Product;
import com.ktb.lookddak.domain.product.entity.ProductItemType;
import lombok.Getter;

@Getter
@JsonPropertyOrder({
        "productId",
        "itemType",
        "productName",
        "productImageUrl",
        "purchaseUrl"
})
public class FittingResultProductResponse {

    private final Long productId;
    private final ProductItemType itemType;
    private final String productName;
    private final String productImageUrl;
    private final String purchaseUrl;

    private FittingResultProductResponse(
            Long productId,
            ProductItemType itemType,
            String productName,
            String productImageUrl,
            String purchaseUrl
    ) {
        this.productId = productId;
        this.itemType = itemType;
        this.productName = productName;
        this.productImageUrl = productImageUrl;
        this.purchaseUrl = purchaseUrl;
    }

    public static FittingResultProductResponse from(Product product) {
        return new FittingResultProductResponse(
                product.getId(),
                product.getItemType(),
                product.getName(),
                product.getImageUrl(),
                product.getPurchaseUrl()
        );
    }
}
