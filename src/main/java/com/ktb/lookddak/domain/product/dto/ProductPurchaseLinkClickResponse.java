package com.ktb.lookddak.domain.product.dto;

import com.ktb.lookddak.domain.product.entity.Product;
import lombok.Getter;

@Getter
public class ProductPurchaseLinkClickResponse {

    private final String purchaseUrl;

    public ProductPurchaseLinkClickResponse(String purchaseUrl) {
        this.purchaseUrl = purchaseUrl;
    }

    public static ProductPurchaseLinkClickResponse from(Product product) {
        return new ProductPurchaseLinkClickResponse(product.getPurchaseUrl());
    }
}
