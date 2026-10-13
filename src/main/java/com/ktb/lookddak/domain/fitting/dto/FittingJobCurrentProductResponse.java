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
        "productImageUrl"
})
public class FittingJobCurrentProductResponse {

    private final Long productId;
    private final ProductItemType itemType;
    private final String productName;
    private final String productImageUrl;

    private FittingJobCurrentProductResponse(
            Long productId,
            ProductItemType itemType,
            String productName,
            String productImageUrl
    ) {
        this.productId = productId;
        this.itemType = itemType;
        this.productName = productName;
        this.productImageUrl = productImageUrl;
    }

    public static FittingJobCurrentProductResponse from(Product product) {
        return new FittingJobCurrentProductResponse(
                product.getId(),
                product.getItemType(),
                product.getName(),
                product.getImageUrl()
        );
    }
}
