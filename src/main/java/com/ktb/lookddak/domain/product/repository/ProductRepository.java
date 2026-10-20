package com.ktb.lookddak.domain.product.repository;

import com.ktb.lookddak.domain.product.entity.Product;
import com.ktb.lookddak.domain.product.dto.ProductRankingQueryResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findAllByProductCodeIn(Collection<String> productCodes);

    Optional<Product> findByProductCode(String productCode);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Product product
            set product.previousPrice = product.currentPrice,
                product.currentPrice = :newPrice
            where product.productCode = :productCode
              and product.currentPrice = :oldPrice
            """)
    int updatePriceIfCurrentPriceMatches(
            @Param("productCode") String productCode,
            @Param("oldPrice") Integer oldPrice,
            @Param("newPrice") Integer newPrice
    );

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
