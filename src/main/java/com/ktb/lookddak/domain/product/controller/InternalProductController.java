package com.ktb.lookddak.domain.product.controller;

import com.ktb.lookddak.domain.product.dto.ProductPriceSyncRequest;
import com.ktb.lookddak.domain.product.dto.ProductPriceSyncResponse;
import com.ktb.lookddak.domain.product.exception.ProductPriceSyncInvalidRequestException;
import com.ktb.lookddak.domain.product.service.ProductPriceSyncService;
import com.ktb.lookddak.global.response.ApiResponse;
import com.ktb.lookddak.global.response.SuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/products")
@RequiredArgsConstructor
public class InternalProductController {

    private static final String INVALID_REQUEST_MESSAGE = "가격 동기화 요청이 올바르지 않습니다.";

    private final ProductPriceSyncService productPriceSyncService;

    @PatchMapping
    public ResponseEntity<ApiResponse<ProductPriceSyncResponse>> syncPrices(
            @RequestBody ProductPriceSyncRequest request
    ) {
        ProductPriceSyncService.ProductPriceSyncResult result = productPriceSyncService.sync(request);
        SuccessCode successCode = result.partialFailure()
                ? SuccessCode.PRODUCT_PRICE_UPDATE_PARTIAL_SUCCESS
                : SuccessCode.PRODUCT_PRICE_UPDATE_SUCCESS;

        return ResponseEntity
                .status(successCode.getStatus())
                .body(ApiResponse.success(successCode, result.response()));
    }

    @ExceptionHandler(ProductPriceSyncInvalidRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidRequest(
            ProductPriceSyncInvalidRequestException exception
    ) {
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.failure("INVALID_REQUEST", INVALID_REQUEST_MESSAGE));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidJson(HttpMessageNotReadableException exception) {
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.failure("INVALID_JSON_FORMAT", "요청 JSON 형식이 올바르지 않습니다."));
    }
}
