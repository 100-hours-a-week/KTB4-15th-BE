package com.ktb.lookddak.domain.product.controller;

import com.ktb.lookddak.domain.product.dto.ProductPurchaseLinkClickResponse;
import com.ktb.lookddak.domain.product.service.ProductService;
import com.ktb.lookddak.global.exception.BusinessException;
import com.ktb.lookddak.global.exception.ErrorCode;
import com.ktb.lookddak.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @Test
    @DisplayName("구매 링크 클릭을 집계하고 구매 URL을 반환한다")
    void recordPurchaseLinkClick() throws Exception {
        MockMvc mockMvc = createMockMvc();
        given(productService.recordPurchaseLinkClick(1L))
                .willReturn(new ProductPurchaseLinkClickResponse(
                        "https://shop.lookddak.com/products/1"
                ));

        mockMvc.perform(post(
                        "/api/v1/products/1/purchase-link-clicks"
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.purchaseUrl")
                        .value("https://shop.lookddak.com/products/1"))
                .andExpect(jsonPath("$.message")
                        .value("요청이 성공적으로 처리되었습니다."));

        verify(productService).recordPurchaseLinkClick(1L);
    }

    @Test
    @DisplayName("존재하지 않는 상품이면 404 Not Found를 반환한다")
    void rejectMissingProduct() throws Exception {
        MockMvc mockMvc = createMockMvc();
        given(productService.recordPurchaseLinkClick(999L))
                .willThrow(new BusinessException(
                        ErrorCode.PRODUCT_NOT_FOUND
                ));

        mockMvc.perform(post(
                        "/api/v1/products/999/purchase-link-clicks"
                ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("PRODUCT_NOT_FOUND"));
    }

    private MockMvc createMockMvc() {
        return MockMvcBuilders
                .standaloneSetup(new ProductController(productService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }
}
