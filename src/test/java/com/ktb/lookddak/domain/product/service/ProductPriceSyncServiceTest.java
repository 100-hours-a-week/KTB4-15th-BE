package com.ktb.lookddak.domain.product.service;

import com.ktb.lookddak.domain.product.dto.ProductPriceSyncItemRequest;
import com.ktb.lookddak.domain.product.dto.ProductPriceSyncRequest;
import com.ktb.lookddak.domain.product.entity.Product;
import com.ktb.lookddak.domain.product.entity.ProductItemType;
import com.ktb.lookddak.domain.product.exception.ProductPriceSyncInvalidRequestException;
import com.ktb.lookddak.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductPriceSyncServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductPriceSyncService productPriceSyncService;

    @Test
    @DisplayName("현재 가격이 oldPrice와 일치하면 가격을 갱신한다")
    void syncPrice() {
        ProductPriceSyncRequest request = request(item("0000001", 89_000, 79_000));
        given(productRepository.updatePriceIfCurrentPriceMatches("0000001", 89_000, 79_000))
                .willReturn(1);

        ProductPriceSyncService.ProductPriceSyncResult result = productPriceSyncService.sync(request);

        assertThat(result.partialFailure()).isFalse();
        assertThat(result.response().receivedCount()).isEqualTo(1);
        assertThat(result.response().notFoundProductCodes()).isEmpty();
        assertThat(result.response().priceConflicts()).isEmpty();
        verify(productRepository).updatePriceIfCurrentPriceMatches("0000001", 89_000, 79_000);
        verify(productRepository, never()).findByProductCode("0000001");
    }

    @Test
    @DisplayName("동일한 가격 변경 요청을 재수신하면 이미 반영된 성공으로 처리한다")
    void acceptReplay() {
        Product product = product("0000001", 79_000);
        ProductPriceSyncRequest request = request(item("0000001", 89_000, 79_000));
        given(productRepository.updatePriceIfCurrentPriceMatches("0000001", 89_000, 79_000))
                .willReturn(0);
        given(productRepository.findByProductCode("0000001"))
                .willReturn(Optional.of(product));

        ProductPriceSyncService.ProductPriceSyncResult result = productPriceSyncService.sync(request);

        assertThat(result.partialFailure()).isFalse();
        assertThat(result.response().notFoundProductCodes()).isEmpty();
        assertThat(result.response().priceConflicts()).isEmpty();
    }

    @Test
    @DisplayName("현재 가격이 oldPrice와 newPrice 모두 다르면 가격을 변경하지 않고 충돌 정보를 반환한다")
    void returnPriceConflictForStalePrice() {
        Product product = product("0000001", 85_000);
        ProductPriceSyncRequest request = request(item("0000001", 89_000, 79_000));
        given(productRepository.updatePriceIfCurrentPriceMatches("0000001", 89_000, 79_000))
                .willReturn(0);
        given(productRepository.findByProductCode("0000001"))
                .willReturn(Optional.of(product));

        ProductPriceSyncService.ProductPriceSyncResult result = productPriceSyncService.sync(request);

        assertThat(result.partialFailure()).isTrue();
        assertThat(result.response().notFoundProductCodes()).isEmpty();
        assertThat(result.response().priceConflicts())
                .containsExactly(new com.ktb.lookddak.domain.product.dto.ProductPriceConflictResponse("0000001", 85_000));
    }

    @Test
    @DisplayName("존재하지 않는 상품 코드는 미존재 코드로 반환한다")
    void returnNotFoundCode() {
        ProductPriceSyncRequest request = request(item("0000001", 89_000, 79_000));
        given(productRepository.updatePriceIfCurrentPriceMatches("0000001", 89_000, 79_000))
                .willReturn(0);
        given(productRepository.findByProductCode("0000001"))
                .willReturn(Optional.empty());

        ProductPriceSyncService.ProductPriceSyncResult result = productPriceSyncService.sync(request);

        assertThat(result.partialFailure()).isTrue();
        assertThat(result.response().notFoundProductCodes()).containsExactly("0000001");
        assertThat(result.response().priceConflicts()).isEmpty();
    }

    @Test
    @DisplayName("변경 개수와 상품 수가 다르면 요청을 거절한다")
    void rejectMismatchedChangedCount() {
        ProductPriceSyncRequest request = new ProductPriceSyncRequest(
                2,
                List.of(item("0000001", 89_000, 79_000))
        );

        assertThatThrownBy(() -> productPriceSyncService.sync(request))
                .isInstanceOf(ProductPriceSyncInvalidRequestException.class);
    }

    @Test
    @DisplayName("중복 상품 코드 요청을 거절한다")
    void rejectDuplicatedProductCode() {
        ProductPriceSyncRequest request = new ProductPriceSyncRequest(
                2,
                List.of(
                        item("0000001", 89_000, 79_000),
                        item("0000001", 79_000, 69_000)
                )
        );

        assertThatThrownBy(() -> productPriceSyncService.sync(request))
                .isInstanceOf(ProductPriceSyncInvalidRequestException.class);
    }

    @Test
    @DisplayName("숫자가 아닌 상품 코드 요청을 거절한다")
    void rejectNonNumericProductCode() {
        ProductPriceSyncRequest request = request(item("ABC123", 89_000, 79_000));

        assertThatThrownBy(() -> productPriceSyncService.sync(request))
                .isInstanceOf(ProductPriceSyncInvalidRequestException.class);
    }

    private ProductPriceSyncRequest request(ProductPriceSyncItemRequest item) {
        return new ProductPriceSyncRequest(1, List.of(item));
    }

    private ProductPriceSyncItemRequest item(String productCode, int oldPrice, int newPrice) {
        return new ProductPriceSyncItemRequest(productCode, oldPrice, newPrice);
    }

    private Product product(String productCode, int currentPrice) {
        return Product.create(
                productCode,
                "테스트 상품",
                "https://image.lookddak.com/products/1.jpg",
                currentPrice,
                "NAVY",
                ProductItemType.TOP,
                "https://shop.lookddak.com/products/1"
        );
    }
}
