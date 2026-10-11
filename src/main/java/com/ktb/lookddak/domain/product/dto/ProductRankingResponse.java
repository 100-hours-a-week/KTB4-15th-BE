package com.ktb.lookddak.domain.product.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.ktb.lookddak.domain.product.entity.ProductRankingType;
import lombok.Getter;

import java.util.List;

@Getter
@JsonPropertyOrder({"type", "items"})
public class ProductRankingResponse {

    private final ProductRankingType type;
    private final List<ProductRankingItemResponse> items;

    public ProductRankingResponse(
            ProductRankingType type,
            List<ProductRankingItemResponse> items
    ) {
        this.type = type;
        this.items = items;
    }
}
