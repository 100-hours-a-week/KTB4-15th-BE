package com.ktb.lookddak.domain.product.controller;

import com.ktb.lookddak.domain.product.dto.ProductPurchaseLinkClickResponse;
import com.ktb.lookddak.domain.product.service.ProductService;
import com.ktb.lookddak.global.response.ApiResponse;
import com.ktb.lookddak.global.response.SuccessCode;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping("/{productId}/purchase-link-clicks")
    public ResponseEntity<ApiResponse<ProductPurchaseLinkClickResponse>>
    recordPurchaseLinkClick(
            @Positive @PathVariable Long productId
    ) {
        ProductPurchaseLinkClickResponse response =
                productService.recordPurchaseLinkClick(productId);

        return ResponseEntity
                .status(SuccessCode.OK.getStatus())
                .body(ApiResponse.success(SuccessCode.OK, response));
    }
}
