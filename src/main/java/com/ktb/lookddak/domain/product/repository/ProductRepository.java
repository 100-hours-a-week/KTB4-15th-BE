package com.ktb.lookddak.domain.product.repository;

import com.ktb.lookddak.domain.product.entity.Product;
import com.ktb.lookddak.domain.product.dto.ProductRankingQueryResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findAllByProductCodeIn(Collection<String> productCodes);

    @Query("""
            select new com.ktb.lookddak.domain.product.dto.ProductRankingQueryResult(
                product,
                product.clickCount
            )
            from Product product
            where product.clickCount > 0
            order by product.clickCount desc, product.id asc
            """)
    List<ProductRankingQueryResult> findClickRankings(Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Product product
            set product.clickCount = product.clickCount + 1
            where product.id = :productId
            """)
    int incrementClickCount(@Param("productId") Long productId);
}
