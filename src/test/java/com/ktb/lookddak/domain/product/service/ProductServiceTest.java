package com.ktb.lookddak.domain.product.service;

import com.ktb.lookddak.domain.product.dto.ProductPurchaseLinkClickResponse;
import com.ktb.lookddak.domain.product.dto.ProductRankingQueryResult;
import com.ktb.lookddak.domain.product.dto.ProductRankingResponse;
import com.ktb.lookddak.domain.product.entity.Product;
import com.ktb.lookddak.domain.product.entity.ProductItemType;
import com.ktb.lookddak.domain.product.entity.ProductRankingType;
import com.ktb.lookddak.domain.product.repository.ProductRepository;
import com.ktb.lookddak.domain.wishlist.repository.WishlistRepository;
import com.ktb.lookddak.global.exception.BusinessException;
import com.ktb.lookddak.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WishlistRepository wishlistRepository;

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

    @Test
    @DisplayName("찜 랭킹 조회 결과에 순위를 부여한다")
    void getWishRankings() {
        Product firstProduct = createProduct();
        Product secondProduct = Product.create(
                "0000002",
                "모던 테이퍼드 슬랙스",
                "https://image.lookddak.com/products/2.jpg",
                59_000,
                "차콜",
                ProductItemType.BOTTOM,
                "https://shop.lookddak.com/products/2"
        );
        given(wishlistRepository.findWishRankings(any()))
                .willReturn(List.of(
                        new ProductRankingQueryResult(firstProduct, 10L),
                        new ProductRankingQueryResult(secondProduct, 8L)
                ));

        ProductRankingResponse response = productService.getRankings("WISH");

        assertThat(response.getType()).isEqualTo(ProductRankingType.WISH);
        assertThat(response.getItems())
                .extracting("rank")
                .containsExactly(1, 2);
        assertThat(response.getItems())
                .extracting("rankingCount")
                .containsExactly(10L, 8L);
        verify(wishlistRepository).findWishRankings(any());
    }

    @Test
    @DisplayName("클릭 랭킹 데이터가 없으면 빈 목록을 반환한다")
    void getEmptyClickRankings() {
        given(productRepository.findClickRankings(any()))
                .willReturn(List.of());

        ProductRankingResponse response = productService.getRankings("CLICK");

        assertThat(response.getType()).isEqualTo(ProductRankingType.CLICK);
        assertThat(response.getItems()).isEmpty();
        verify(productRepository).findClickRankings(any());
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
