package com.ktb.lookddak.domain.product.service;

import com.ktb.lookddak.domain.product.dto.ProductPurchaseLinkClickResponse;
import com.ktb.lookddak.domain.product.entity.Product;
import com.ktb.lookddak.domain.product.entity.ProductItemType;
import com.ktb.lookddak.domain.product.repository.ProductRepository;
import com.ktb.lookddak.global.exception.BusinessException;
import com.ktb.lookddak.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    @DisplayName("구매 링크 클릭 수를 증가시키고 구매 URL을 반환한다")
    void recordPurchaseLinkClick() {
        Product product = createProduct();
        given(productRepository.incrementClickCount(1L)).willReturn(1);
        given(productRepository.findById(1L))
                .willReturn(Optional.of(product));

        ProductPurchaseLinkClickResponse response =
                productService.recordPurchaseLinkClick(1L);

        assertThat(response.getPurchaseUrl())
                .isEqualTo("https://shop.lookddak.com/products/1");
        verify(productRepository).incrementClickCount(1L);
        verify(productRepository).findById(1L);
    }

    @Test
    @DisplayName("존재하지 않는 상품의 구매 링크 클릭은 집계하지 않는다")
    void rejectMissingProduct() {
        given(productRepository.incrementClickCount(999L)).willReturn(0);

        assertThatThrownBy(() ->
                productService.recordPurchaseLinkClick(999L)
        )
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }

    private Product createProduct() {
        return Product.create(
                "0000001",
                "에센셜 램스울 크루넥",
                "https://image.lookddak.com/products/1.jpg",
                49_000,
                "네이비",
                ProductItemType.TOP,
                "https://shop.lookddak.com/products/1"
        );
    }
}
