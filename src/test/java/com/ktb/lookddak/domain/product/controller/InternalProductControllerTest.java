package com.ktb.lookddak.domain.product.controller;

import com.ktb.lookddak.domain.product.dto.ProductPriceSyncResponse;
import com.ktb.lookddak.domain.product.exception.ProductPriceSyncInvalidRequestException;
import com.ktb.lookddak.domain.product.service.ProductPriceSyncService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InternalProductControllerTest {

    @Mock
    private ProductPriceSyncService productPriceSyncService;

    @Test
    @DisplayName("가격 동기화 전체 성공 응답을 snake_case로 반환한다")
    void syncPrices() throws Exception {
        given(productPriceSyncService.sync(any()))
                .willReturn(new ProductPriceSyncService.ProductPriceSyncResult(
                        new ProductPriceSyncResponse(1, List.of(), List.of()),
                        false
                ));

        mockMvc().perform(patch("/api/v1/internal/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "changed_count": 1,
                                  "products": [{
                                    "product_code": "2027362",
                                    "old_price": 89000,
                                    "new_price": 79000
                                  }]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PRODUCT_PRICE_UPDATE_SUCCESS"))
                .andExpect(jsonPath("$.data.received_count").value(1))
                .andExpect(jsonPath("$.data.not_found_product_codes").isEmpty())
                .andExpect(jsonPath("$.data.price_conflicts").isEmpty());
    }

    @Test
    @DisplayName("일부 실패면 부분 성공 응답을 반환한다")
    void returnPartialSuccess() throws Exception {
        given(productPriceSyncService.sync(any()))
                .willReturn(new ProductPriceSyncService.ProductPriceSyncResult(
                        new ProductPriceSyncResponse(
                                2,
                                List.of("4044872"),
                                List.of(new com.ktb.lookddak.domain.product.dto.ProductPriceConflictResponse("2027362", 80_000))
                        ),
                        true
                ));

        mockMvc().perform(patch("/api/v1/internal/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"changed_count\":2," +
                                "\"products\":[{" +
                                "\"product_code\":\"2027362\"," +
                                "\"old_price\":89000," +
                                "\"new_price\":79000},{" +
                                "\"product_code\":\"4044872\"," +
                                "\"old_price\":69000," +
                                "\"new_price\":59000}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PRODUCT_PRICE_UPDATE_PARTIAL_SUCCESS"))
                .andExpect(jsonPath("$.data.not_found_product_codes[0]").value("4044872"))
                .andExpect(jsonPath("$.data.price_conflicts[0].product_code").value("2027362"))
                .andExpect(jsonPath("$.data.price_conflicts[0].current_price").value(80_000));
    }

    @Test
    @DisplayName("잘못된 가격 동기화 요청은 INVALID_REQUEST를 반환한다")
    void rejectInvalidRequest() throws Exception {
        given(productPriceSyncService.sync(any()))
                .willThrow(new ProductPriceSyncInvalidRequestException());

        mockMvc().perform(patch("/api/v1/internal/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"changed_count\":0,\"products\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("JSON 타입이 올바르지 않으면 INVALID_JSON_FORMAT을 반환한다")
    void rejectInvalidJsonFormat() throws Exception {
        mockMvc().perform(patch("/api/v1/internal/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"changed_count\":\"one\",\"products\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_JSON_FORMAT"));
    }

    private MockMvc mockMvc() {
        return MockMvcBuilders
                .standaloneSetup(new InternalProductController(productPriceSyncService))
                .build();
    }
}
