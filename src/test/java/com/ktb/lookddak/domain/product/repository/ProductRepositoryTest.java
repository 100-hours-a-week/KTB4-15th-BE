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

    @Test
    @DisplayName("구매 링크 클릭 수를 원자적으로 1 증가시킨다")
    void incrementClickCount() {
        Product product = productRepository.saveAndFlush(
                createProduct("0000001")
        );
        entityManager.clear();

        int firstUpdatedRowCount = productRepository.incrementClickCount(
                product.getId()
        );
        int secondUpdatedRowCount = productRepository.incrementClickCount(
                product.getId()
        );
        entityManager.flush();
        entityManager.clear();

        Product updatedProduct = productRepository.findById(product.getId())
                .orElseThrow();
        assertThat(firstUpdatedRowCount).isEqualTo(1);
        assertThat(secondUpdatedRowCount).isEqualTo(1);
        assertThat(updatedProduct.getClickCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("존재하지 않는 상품의 클릭 수는 증가시키지 않는다")
    void doNotIncrementMissingProduct() {
        int updatedRowCount = productRepository.incrementClickCount(999L);

        assertThat(updatedRowCount).isZero();
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
