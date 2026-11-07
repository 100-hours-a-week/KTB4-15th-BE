package com.ktb.lookddak.domain.product.repository;

import com.ktb.lookddak.domain.product.entity.ProductClickReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ProductClickReceiptRepository extends JpaRepository<ProductClickReceipt, Long> {

    // 유니크 키로 최초 클릭부터 동일 회원·상품의 동시 요청을 직렬화한다.
    // 중복 키일 때의 영향 행 수는 DB 설정에 따라 다르므로 집계 여부 판단에 사용하지 않는다.
    @Modifying
    @Query(value = """
            insert into product_click_receipt (member_id, product_id)
            values (:memberId, :productId)
            on duplicate key update member_id = :memberId
            """, nativeQuery = true)
    void ensureReceipt(
            @Param("memberId") Long memberId,
            @Param("productId") Long productId
    );

    @Modifying
    @Query("""
            update ProductClickReceipt receipt
            set receipt.lastCountedAt = :now
            where receipt.memberId = :memberId
              and receipt.productId = :productId
              and (receipt.lastCountedAt is null or receipt.lastCountedAt <= :cutoff)
            """)
    int claimClick(
            @Param("memberId") Long memberId,
            @Param("productId") Long productId,
            @Param("now") LocalDateTime now,
            @Param("cutoff") LocalDateTime cutoff
    );
}
