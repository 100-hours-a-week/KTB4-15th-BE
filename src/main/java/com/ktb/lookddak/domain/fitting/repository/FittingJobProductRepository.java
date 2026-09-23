package com.ktb.lookddak.domain.fitting.repository;

import com.ktb.lookddak.domain.fitting.entity.FittingJobProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FittingJobProductRepository
        extends JpaRepository<FittingJobProduct, Long> {

    @Query("""
            select fittingJobProduct
            from FittingJobProduct fittingJobProduct
            join fetch fittingJobProduct.product product
            where fittingJobProduct.fittingJob.id = :fittingJobId
            order by fittingJobProduct.id asc
            """)
    List<FittingJobProduct> findAllByFittingJobIdWithProduct(
            @Param("fittingJobId") Long fittingJobId
    );
}
