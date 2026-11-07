package com.ktb.lookddak.domain.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "product_click_receipt",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_product_click_member_product",
                columnNames = {"member_id", "product_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductClickReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    // 마지막으로 집계한 UTC 시각이며, 중복 클릭으로는 갱신하지 않는다.
    @Column(name = "last_counted_at")
    private LocalDateTime lastCountedAt;
}
