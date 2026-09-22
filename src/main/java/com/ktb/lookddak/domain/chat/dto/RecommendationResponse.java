package com.ktb.lookddak.domain.chat.dto;

import com.ktb.lookddak.domain.recommendation.entity.Recommendation;
import lombok.Getter;

import java.util.List;

@Getter
public class RecommendationResponse {

    private final Long recommendationId;
    private final List<RecommendedProductResponse> products;

    private RecommendationResponse(
            Long recommendationId,
            List<RecommendedProductResponse> products
    ) {
        this.recommendationId = recommendationId;
        this.products = List.copyOf(products);
    }

    public static RecommendationResponse from(
            Recommendation recommendation,
            List<RecommendedProductResponse> products
    ) {
        return new RecommendationResponse(recommendation.getId(), products);
    }
}
