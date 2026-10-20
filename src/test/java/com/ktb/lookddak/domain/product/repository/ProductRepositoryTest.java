package com.ktb.lookddak.domain.product.repository;

import com.ktb.lookddak.domain.product.entity.Product;
import com.ktb.lookddak.domain.product.entity.ProductItemType;
import com.ktb.lookddak.domain.product.dto.ProductRankingQueryResult;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

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

    @Test
    @DisplayName("현재 가격이 일치할 때만 직전 가격과 현재 가격을 함께 갱신한다")
    void updatePriceIfCurrentPriceMatches() {
        Product product = productRepository.saveAndFlush(createProduct("0000001"));
        entityManager.clear();

        int updatedRowCount = productRepository.updatePriceIfCurrentPriceMatches(
                "0000001",
                49_000,
                39_000
        );
        entityManager.flush();
        entityManager.clear();

        Product updatedProduct = productRepository.findById(product.getId())
                .orElseThrow();
        assertThat(updatedRowCount).isEqualTo(1);
        assertThat(updatedProduct.getPreviousPrice()).isEqualTo(49_000);
        assertThat(updatedProduct.getCurrentPrice()).isEqualTo(39_000);
    }

    @Test
    @DisplayName("현재 가격이 일치하지 않으면 가격을 갱신하지 않는다")
    void doNotUpdatePriceWhenCurrentPriceDoesNotMatch() {
        Product product = productRepository.saveAndFlush(createProduct("0000001"));

        int updatedRowCount = productRepository.updatePriceIfCurrentPriceMatches(
                "0000001",
                50_000,
                39_000
        );
        entityManager.clear();

        Product unchangedProduct = productRepository.findById(product.getId())
                .orElseThrow();
        assertThat(updatedRowCount).isZero();
        assertThat(unchangedProduct.getPreviousPrice()).isNull();
        assertThat(unchangedProduct.getCurrentPrice()).isEqualTo(49_000);
    }

    @Test
    @DisplayName("클릭 수가 있는 상품만 클릭 수 내림차순과 상품 ID 오름차순으로 조회한다")
    void findClickRankings() {
        Product firstProduct = productRepository.saveAndFlush(
                createProduct("0000001")
        );
        Product secondProduct = productRepository.saveAndFlush(
                createProduct("0000002")
        );
        productRepository.saveAndFlush(createProduct("0000003"));

        for (int index = 0; index < 2; index++) {
            productRepository.incrementClickCount(firstProduct.getId());
            productRepository.incrementClickCount(secondProduct.getId());
        }
        productRepository.incrementClickCount(firstProduct.getId());
        entityManager.clear();

        List<ProductRankingQueryResult> rankings = productRepository
                .findClickRankings(PageRequest.of(0, 100));

        assertThat(rankings)
                .extracting(result -> result.product().getId())
                .containsExactly(firstProduct.getId(), secondProduct.getId());
        assertThat(rankings)
                .extracting(ProductRankingQueryResult::rankingCount)
                .containsExactly(3L, 2L);
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
