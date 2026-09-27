package com.ktb.lookddak.domain.fitting.controller;

import com.ktb.lookddak.domain.fitting.dto.FittingCandidateCreateResponse;
import com.ktb.lookddak.domain.fitting.dto.FittingCandidateBulkDeleteResponse;
import com.ktb.lookddak.domain.fitting.dto.FittingCandidateListItemResponse;
import com.ktb.lookddak.domain.fitting.dto.FittingCandidateListResponse;
import com.ktb.lookddak.domain.fitting.entity.FittingCandidate;
import com.ktb.lookddak.domain.fitting.service.FittingCandidateService;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.product.entity.Product;
import com.ktb.lookddak.domain.product.entity.ProductItemType;
import com.ktb.lookddak.global.exception.BusinessException;
import com.ktb.lookddak.global.exception.ErrorCode;
import com.ktb.lookddak.global.exception.GlobalExceptionHandler;
import com.ktb.lookddak.global.security.principal.MemberPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FittingCandidateControllerTest {

    @Mock
    private FittingCandidateService fittingCandidateService;

    @Mock
    private MemberPrincipal principal;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new FittingCandidateController(fittingCandidateService)
                )
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(
                        new AuthenticationPrincipalArgumentResolver()
                )
                .build();
        lenient().when(principal.getMemberId()).thenReturn(1L);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        null,
                        List.of()
                )
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("피팅 후보를 추가하면 201 Created를 반환한다")
    void createFittingCandidate() throws Exception {
        given(fittingCandidateService.createFittingCandidate(eq(1L), any()))
                .willReturn(new FittingCandidateCreateResponse(25L, 10L));

        mockMvc.perform(post("/api/v1/fitting-candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId": 10}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CREATED"))
                .andExpect(jsonPath("$.data.fittingCandidateId").value(25))
                .andExpect(jsonPath("$.data.productId").value(10));
    }

    @Test
    @DisplayName("Query Parameter가 없으면 전체 피팅 후보 첫 페이지를 반환한다")
    void getFirstFittingCandidatePage() throws Exception {
        FittingCandidateListItemResponse item =
                FittingCandidateListItemResponse.from(
                        createCandidate(30L, 101L, ProductItemType.TOP)
                );
        given(fittingCandidateService.getFittingCandidates(
                1L,
                null,
                null,
                20
        )).willReturn(new FittingCandidateListResponse(
                3L,
                List.of(item),
                30L,
                true
        ));

        mockMvc.perform(get("/api/v1/fitting-candidates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.items[0].fittingCandidateId")
                        .value(30))
                .andExpect(jsonPath("$.data.items[0].productId").value(101))
                .andExpect(jsonPath("$.data.items[0].productName")
                        .value("에센셜 램스울 크루넥"))
                .andExpect(jsonPath("$.data.items[0].productImageUrl")
                        .value("https://image.lookddak.com/products/101.jpg"))
                .andExpect(jsonPath("$.data.items[0].currentPrice")
                        .value(49_000))
                .andExpect(jsonPath("$.data.items[0].color").value("차콜"))
                .andExpect(jsonPath("$.data.items[0].itemType").value("TOP"))
                .andExpect(jsonPath("$.data.items[0].saleStatus")
                        .doesNotExist())
                .andExpect(jsonPath("$.data.totalCount").value(3))
                .andExpect(jsonPath("$.data.nextCursor").value(30))
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andExpect(jsonPath("$.message")
                        .value("요청이 성공적으로 처리되었습니다."));

        verify(fittingCandidateService).getFittingCandidates(
                1L,
                null,
                null,
                20
        );
    }

    @Test
    @DisplayName("상품 타입과 Cursor, 조회 크기로 다음 페이지를 조회한다")
    void getNextPageByItemType() throws Exception {
        given(fittingCandidateService.getFittingCandidates(
                1L,
                ProductItemType.BOTTOM,
                25L,
                10
        )).willReturn(new FittingCandidateListResponse(
                0L,
                List.of(),
                null,
                false
        ));

        mockMvc.perform(get("/api/v1/fitting-candidates")
                        .queryParam("itemType", "BOTTOM")
                        .queryParam("cursor", "25")
                        .queryParam("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.totalCount").value(0))
                .andExpect(jsonPath("$.data.nextCursor").isEmpty())
                .andExpect(jsonPath("$.data.hasNext").value(false));

        verify(fittingCandidateService).getFittingCandidates(
                1L,
                ProductItemType.BOTTOM,
                25L,
                10
        );
    }

    @Test
    @DisplayName("지원하지 않는 상품 타입은 공통 입력값 오류를 반환한다")
    void rejectInvalidItemType() throws Exception {
        mockMvc.perform(get("/api/v1/fitting-candidates")
                        .queryParam("itemType", "SHOES"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.data").isEmpty());

        verifyNoInteractions(fittingCandidateService);
    }

    @Test
    @DisplayName("잘못된 Pagination 조건은 400 Bad Request를 반환한다")
    void rejectInvalidPaginationParameter() throws Exception {
        given(fittingCandidateService.getFittingCandidates(
                1L,
                null,
                0L,
                20
        )).willThrow(new BusinessException(
                ErrorCode.INVALID_PAGINATION_PARAMETER
        ));

        mockMvc.perform(get("/api/v1/fitting-candidates")
                        .queryParam("cursor", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_PAGINATION_PARAMETER"))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.message")
                        .value("올바른 조회 조건을 입력해주세요."));
    }

    @Test
    @DisplayName("상품 ID가 양수가 아니면 400 Bad Request를 반환한다")
    void rejectInvalidProductId() throws Exception {
        mockMvc.perform(post("/api/v1/fitting-candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId": 0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    @DisplayName("후보 제한을 초과하면 409 Conflict를 반환한다")
    void rejectCandidateLimit() throws Exception {
        given(fittingCandidateService.createFittingCandidate(eq(1L), any()))
                .willThrow(new BusinessException(
                        ErrorCode.FITTING_CANDIDATE_LIMIT_EXCEEDED
                ));

        mockMvc.perform(post("/api/v1/fitting-candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId": 10}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("FITTING_CANDIDATE_LIMIT_EXCEEDED"));
    }

    @Test
    @DisplayName("피팅 후보를 삭제하면 200 OK를 반환한다")
    void deleteFittingCandidate() throws Exception {
        mockMvc.perform(delete("/api/v1/fitting-candidates/25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(fittingCandidateService).deleteFittingCandidate(1L, 25L);
    }

    @Test
    @DisplayName("피팅 후보 여러 개를 삭제하면 삭제 개수와 200 OK를 반환한다")
    void deleteFittingCandidates() throws Exception {
        given(fittingCandidateService.deleteFittingCandidates(eq(1L), any()))
                .willReturn(new FittingCandidateBulkDeleteResponse(3));

        mockMvc.perform(delete("/api/v1/fitting-candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fittingCandidateIds": [21, 22, 23]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.deletedCount").value(3))
                .andExpect(jsonPath("$.message")
                        .value("요청이 성공적으로 처리되었습니다."));

        verify(fittingCandidateService)
                .deleteFittingCandidates(eq(1L), any());
    }

    @Test
    @DisplayName("다건 삭제 ID 목록이 비어 있으면 400 Bad Request를 반환한다")
    void rejectEmptyBulkDeleteRequest() throws Exception {
        mockMvc.perform(delete("/api/v1/fitting-candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fittingCandidateIds": []}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_INPUT_VALUE"));

        verifyNoInteractions(fittingCandidateService);
    }

    private FittingCandidate createCandidate(
            Long candidateId,
            Long productId,
            ProductItemType itemType
    ) {
        Member member = Member.create(
                "member@lookddak.com",
                "encoded-password"
        );
        Product product = Product.create(
                "product-" + productId,
                "에센셜 램스울 크루넥",
                "https://image.lookddak.com/products/101.jpg",
                49_000,
                "차콜",
                itemType,
                "https://shop.lookddak.com/products/101"
        );
        ReflectionTestUtils.setField(product, "id", productId);

        FittingCandidate candidate = FittingCandidate.create(member, product);
        ReflectionTestUtils.setField(candidate, "id", candidateId);
        return candidate;
    }
}
