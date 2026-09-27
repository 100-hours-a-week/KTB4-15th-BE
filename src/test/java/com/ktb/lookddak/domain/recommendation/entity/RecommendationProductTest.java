package com.ktb.lookddak.domain.recommendation.entity;

import com.ktb.lookddak.domain.product.entity.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RecommendationProductTest {

    @Test
    @DisplayName("추천과 상품을 연결한 추천 상품을 생성한다")
    void createRecommendationProduct() {
        Recommendation recommendation = mock(Recommendation.class);
        Product product = mock(Product.class);

        RecommendationProduct recommendationProduct =
                RecommendationProduct.create(
                        recommendation,
                        product,
                        59_000,
                        "추천 당시 이유"
                );

        assertThat(recommendationProduct.getRecommendation())
                .isSameAs(recommendation);
        assertThat(recommendationProduct.getProduct()).isSameAs(product);
        assertThat(recommendationProduct.getPriceSnapshot()).isEqualTo(59_000);
        assertThat(recommendationProduct.getRecommendedReason())
                .isEqualTo("추천 당시 이유");
    }
}
