package com.ktb.lookddak.domain.fitting.entity;

import com.ktb.lookddak.domain.product.entity.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class FittingJobProductTest {

    @Test
    @DisplayName("가상피팅 작업과 상품을 연결한다")
    void createFittingJobProduct() {
        FittingJob fittingJob = mock(FittingJob.class);
        Product product = mock(Product.class);

        FittingJobProduct fittingJobProduct =
                FittingJobProduct.create(fittingJob, product);

        assertThat(fittingJobProduct.getFittingJob()).isSameAs(fittingJob);
        assertThat(fittingJobProduct.getProduct()).isSameAs(product);
    }
}
