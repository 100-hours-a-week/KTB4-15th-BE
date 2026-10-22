package com.ktb.lookddak.domain.product.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProductPriceConflictResponse(
        @JsonProperty("product_code") String productCode,
        @JsonProperty("current_price") Integer currentPrice
) {
}
