package com.ktb.lookddak.domain.product.dto;

import com.ktb.lookddak.domain.product.entity.Product;

public record ProductRankingQueryResult(
        Product product,
        Long rankingCount
) {
}
