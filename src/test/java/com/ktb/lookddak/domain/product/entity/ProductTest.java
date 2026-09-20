package com.ktb.lookddak.domain.product.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductTest {

    @Test
    @DisplayName("현재 가격으로 상품을 생성한다")
    void createProduct() {
        Product product = Product.create(49_000);

        assertThat(product.getCurrentPrice()).isEqualTo(49_000);
    }
}
