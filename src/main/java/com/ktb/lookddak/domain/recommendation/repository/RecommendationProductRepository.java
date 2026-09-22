package com.ktb.lookddak.domain.recommendation.repository;

import com.ktb.lookddak.domain.recommendation.entity.RecommendationProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface RecommendationProductRepository
        extends JpaRepository<RecommendationProduct, Long> {

    @Query("""
            select recommendationProduct
            from RecommendationProduct recommendationProduct
            join fetch recommendationProduct.product
            where recommendationProduct.recommendation.id in :recommendationIds
            order by recommendationProduct.id asc
            """)
    List<RecommendationProduct> findAllWithProductByRecommendationIdIn(
            @Param("recommendationIds") Collection<Long> recommendationIds
    );
}
