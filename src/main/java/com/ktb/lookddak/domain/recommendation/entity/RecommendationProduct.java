package com.ktb.lookddak.domain.recommendation.entity;

import com.ktb.lookddak.domain.product.entity.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "recommendation_product",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_recommendation_product",
                columnNames = {"recommendation_id", "product_id"}
        )
)
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendationProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recommendation_id", nullable = false)
    private Recommendation recommendation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "price_snapshot", nullable = false)
    private Integer priceSnapshot;

    @Column(name = "recommended_reason", nullable = false, columnDefinition = "TEXT")
    private String recommendedReason;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private RecommendationProduct(
            Recommendation recommendation,
            Product product,
            Integer priceSnapshot,
            String recommendedReason
    ) {
        this.recommendation = recommendation;
        this.product = product;
        this.priceSnapshot = priceSnapshot;
        this.recommendedReason = recommendedReason;
    }

    public static RecommendationProduct create(
            Recommendation recommendation,
            Product product,
            Integer priceSnapshot,
            String recommendedReason
    ) {
        return new RecommendationProduct(
                recommendation,
                product,
                priceSnapshot,
                recommendedReason
        );
    }
}
