package com.ktb.lookddak.domain.product.repository;

import com.ktb.lookddak.domain.product.entity.Product;
import com.ktb.lookddak.domain.product.entity.ProductItemType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("상품 코드 목록에 해당하는 상품을 한 번에 조회한다")
    void findAllByProductCodeIn() {
        Product firstProduct = productRepository.save(createProduct("0000001"));
        Product secondProduct = productRepository.save(createProduct("0000002"));
        productRepository.save(createProduct("0000003"));
        entityManager.flush();
        entityManager.clear();

        List<Product> products = productRepository.findAllByProductCodeIn(
                List.of("0000001", "0000002")
        );

        assertThat(products)
                .extracting(Product::getId)
                .containsExactlyInAnyOrder(
                        firstProduct.getId(),
                        secondProduct.getId()
                );
    }

    @Test
    @DisplayName("동일한 상품 코드를 중복 저장할 수 없다")
    void rejectDuplicatedProductCode() {
        productRepository.saveAndFlush(createProduct("0000001"));

        assertThatThrownBy(() ->
                productRepository.saveAndFlush(createProduct("0000001"))
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    private Product createProduct(String productCode) {
        return Product.create(
                productCode,
                "테스트 상품 " + productCode,
                "https://image.lookddak.com/" + productCode + ".jpg",
                49_000,
                "NAVY",
                ProductItemType.TOP,
                "https://shop.lookddak.com/products/" + productCode
        );
    }
}
