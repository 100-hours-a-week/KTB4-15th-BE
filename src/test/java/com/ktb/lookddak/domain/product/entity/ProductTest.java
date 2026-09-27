package com.ktb.lookddak.domain.product.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductTest {

    @Test
    @DisplayName("추천 카드에 필요한 정보로 상품을 생성한다")
    void createProduct() {
        Product product = Product.create(
                "0000001",
                "에센셜 램스울 크루넥",
                "https://image.lookddak.com/products/1.jpg",
                49_000,
                "네이비",
                ProductItemType.TOP,
                "https://shop.lookddak.com/products/1"
        );

        assertThat(product.getProductCode()).isEqualTo("0000001");
        assertThat(product.getName()).isEqualTo("에센셜 램스울 크루넥");
        assertThat(product.getImageUrl())
                .isEqualTo("https://image.lookddak.com/products/1.jpg");
        assertThat(product.getCurrentPrice()).isEqualTo(49_000);
        assertThat(product.getColor()).isEqualTo("네이비");
        assertThat(product.getItemType()).isEqualTo(ProductItemType.TOP);
        assertThat(product.getPurchaseUrl())
                .isEqualTo("https://shop.lookddak.com/products/1");
    }
}
