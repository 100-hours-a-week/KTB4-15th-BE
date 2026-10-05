package com.ktb.lookddak.domain.product.service;

import com.ktb.lookddak.domain.product.dto.ProductPurchaseLinkClickResponse;
import com.ktb.lookddak.domain.product.entity.Product;
import com.ktb.lookddak.domain.product.repository.ProductRepository;
import com.ktb.lookddak.global.exception.BusinessException;
import com.ktb.lookddak.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

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
