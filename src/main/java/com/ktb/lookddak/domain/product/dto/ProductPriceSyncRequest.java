package com.ktb.lookddak.domain.product.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductPriceSyncRequest {

    @JsonProperty("changed_count")
    private Integer changedCount;

    private List<ProductPriceSyncItemRequest> products;

    public ProductPriceSyncRequest(
            Integer changedCount,
            List<ProductPriceSyncItemRequest> products
    ) {
        this.changedCount = changedCount;
        this.products = products;
    }
}
