package com.ktb.lookddak.domain.product.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ProductPriceSyncResponse(
        @JsonProperty("received_count") int receivedCount,
        @JsonProperty("not_found_product_codes") List<String> notFoundProductCodes,
        @JsonProperty("price_conflicts") List<ProductPriceConflictResponse> priceConflicts
) {
}
