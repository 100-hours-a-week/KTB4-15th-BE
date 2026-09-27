package com.ktb.lookddak.domain.fitting.entity;

import com.ktb.lookddak.domain.product.entity.Product;
import jakarta.persistence.Entity;
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

@Getter
@Entity
@Table(
        name = "fitting_job_product",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_fitting_job_product",
                columnNames = {"fitting_job_id", "product_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FittingJobProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fitting_job_id", nullable = false)
    private FittingJob fittingJob;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    private FittingJobProduct(FittingJob fittingJob, Product product) {
        this.fittingJob = fittingJob;
        this.product = product;
    }

    public static FittingJobProduct create(
            FittingJob fittingJob,
            Product product
    ) {
        return new FittingJobProduct(fittingJob, product);
    }
}
