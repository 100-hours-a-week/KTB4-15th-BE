package com.ktb.lookddak.domain.product.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductPriceSyncItemRequest {

    @JsonProperty("product_code")
    private String productCode;

    @JsonProperty("old_price")
    private Integer oldPrice;

    @JsonProperty("new_price")
    private Integer newPrice;

    public ProductPriceSyncItemRequest(
            String productCode,
            Integer oldPrice,
            Integer newPrice
    ) {
        this.productCode = productCode;
        this.oldPrice = oldPrice;
        this.newPrice = newPrice;
    }
}
