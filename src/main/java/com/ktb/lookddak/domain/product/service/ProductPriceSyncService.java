package com.ktb.lookddak.domain.product.service;

import com.ktb.lookddak.domain.product.dto.ProductPriceSyncItemRequest;
import com.ktb.lookddak.domain.product.dto.ProductPriceSyncRequest;
import com.ktb.lookddak.domain.product.dto.ProductPriceSyncResponse;
import com.ktb.lookddak.domain.product.entity.Product;
import com.ktb.lookddak.domain.product.exception.ProductPriceSyncInvalidRequestException;
import com.ktb.lookddak.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductPriceSyncService {

    private static final int MAX_BATCH_SIZE = 100;
    private static final int MAX_PRODUCT_CODE_LENGTH = 50;

    private final ProductRepository productRepository;

    @Transactional
    public ProductPriceSyncResult sync(ProductPriceSyncRequest request) {
        validate(request);

        List<String> failedProductCodes = new ArrayList<>();
        List<String> notFoundProductCodes = new ArrayList<>();

        for (ProductPriceSyncItemRequest item : request.getProducts()) {
            int updatedCount = productRepository.updatePriceIfCurrentPriceMatches(
                    item.getProductCode(),
                    item.getOldPrice(),
                    item.getNewPrice()
            );

            if (updatedCount == 1) {
                continue;
            }

            classifyUnchangedItem(item, failedProductCodes, notFoundProductCodes);
        }

        ProductPriceSyncResponse response = new ProductPriceSyncResponse(
                request.getProducts().size(),
                List.copyOf(failedProductCodes),
                List.copyOf(notFoundProductCodes)
        );

        return new ProductPriceSyncResult(
                response,
                !failedProductCodes.isEmpty() || !notFoundProductCodes.isEmpty()
        );
    }

    private void classifyUnchangedItem(
            ProductPriceSyncItemRequest item,
            List<String> failedProductCodes,
            List<String> notFoundProductCodes
    ) {
        Product product = productRepository.findByProductCode(item.getProductCode())
                .orElse(null);

        if (product == null) {
            notFoundProductCodes.add(item.getProductCode());
            return;
        }

        if (!product.getCurrentPrice().equals(item.getNewPrice())) {
            failedProductCodes.add(item.getProductCode());
        }
    }

    private void validate(ProductPriceSyncRequest request) {
        if (request == null
                || request.getChangedCount() == null
                || request.getProducts() == null
                || request.getProducts().isEmpty()
                || request.getProducts().size() > MAX_BATCH_SIZE
                || !request.getChangedCount().equals(request.getProducts().size())) {
            throw new ProductPriceSyncInvalidRequestException();
        }

        Set<String> productCodes = new HashSet<>();
        for (ProductPriceSyncItemRequest item : request.getProducts()) {
            if (item == null
                    || item.getProductCode() == null
                    || item.getProductCode().isBlank()
                    || item.getProductCode().length() > MAX_PRODUCT_CODE_LENGTH
                    || item.getOldPrice() == null
                    || item.getNewPrice() == null
                    || item.getOldPrice() <= 0
                    || item.getNewPrice() <= 0
                    || item.getOldPrice().equals(item.getNewPrice())
                    || !productCodes.add(item.getProductCode())) {
                throw new ProductPriceSyncInvalidRequestException();
            }
        }
    }

    public record ProductPriceSyncResult(
            ProductPriceSyncResponse response,
            boolean partialFailure
    ) {
    }
}
