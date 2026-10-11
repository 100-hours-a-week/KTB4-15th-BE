package com.ktb.lookddak.domain.product.service;

import com.ktb.lookddak.domain.product.dto.ProductPurchaseLinkClickResponse;
import com.ktb.lookddak.domain.product.dto.ProductRankingItemResponse;
import com.ktb.lookddak.domain.product.dto.ProductRankingQueryResult;
import com.ktb.lookddak.domain.product.dto.ProductRankingResponse;
import com.ktb.lookddak.domain.product.entity.Product;
import com.ktb.lookddak.domain.product.entity.ProductRankingType;
import com.ktb.lookddak.domain.product.repository.ProductRepository;
import com.ktb.lookddak.domain.wishlist.repository.WishlistRepository;
import com.ktb.lookddak.global.exception.BusinessException;
import com.ktb.lookddak.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private static final int MAX_RANKING_SIZE = 100;

    private final ProductRepository productRepository;
    private final WishlistRepository wishlistRepository;

    public ProductRankingResponse getRankings(String type) {
        ProductRankingType rankingType = ProductRankingType.from(type);
        List<ProductRankingQueryResult> rankingResults = switch (rankingType) {
            case WISH -> wishlistRepository.findWishRankings(
                    PageRequest.of(0, MAX_RANKING_SIZE)
            );
            case CLICK -> productRepository.findClickRankings(
                    PageRequest.of(0, MAX_RANKING_SIZE)
            );
        };

        List<ProductRankingItemResponse> items = new ArrayList<>();
        for (int index = 0; index < rankingResults.size(); index++) {
            items.add(ProductRankingItemResponse.from(
                    index + 1,
                    rankingResults.get(index)
            ));
        }

        return new ProductRankingResponse(rankingType, items);
    }

    @Transactional
    public ProductPurchaseLinkClickResponse recordPurchaseLinkClick(
            Long productId
    ) {
        int updatedRowCount = productRepository.incrementClickCount(productId);
        if (updatedRowCount == 0) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.PRODUCT_NOT_FOUND)
                );

        return ProductPurchaseLinkClickResponse.from(product);
    }
}
