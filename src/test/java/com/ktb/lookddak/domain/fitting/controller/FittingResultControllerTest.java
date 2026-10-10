package com.ktb.lookddak.domain.fitting.controller;

import com.ktb.lookddak.domain.fitting.dto.FittingResultListResponse;
import com.ktb.lookddak.domain.fitting.service.FittingResultService;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FittingResultControllerTest {

    @Mock
    private FittingResultService fittingResultService;

    @Mock
    private MemberPrincipal principal;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new FittingResultController(fittingResultService)
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
    @DisplayName("저장된 가상피팅 결과 첫 페이지를 조회하면 200 OK를 반환한다")
    void getFirstFittingResultPage() throws Exception {
        given(fittingResultService.getFittingResults(1L, null, 20))
                .willReturn(new FittingResultListResponse(
                        List.of(),
                        0L,
                        null,
                        false
                ));

        mockMvc.perform(get("/api/v1/fitting-results"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.totalCount").value(0))
                .andExpect(jsonPath("$.data.nextCursor").isEmpty())
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.message")
                        .value("요청이 성공적으로 처리되었습니다."));

        verify(fittingResultService).getFittingResults(1L, null, 20);
    }

    @Test
    @DisplayName("Cursor와 size를 전달하면 다음 페이지를 조회한다")
    void getNextFittingResultPage() throws Exception {
        given(fittingResultService.getFittingResults(1L, 25L, 10))
                .willReturn(new FittingResultListResponse(
                        List.of(),
                        2L,
                        null,
                        false
                ));

        mockMvc.perform(get("/api/v1/fitting-results")
                        .queryParam("cursor", "25")
                        .queryParam("size", "10"))
                .andExpect(status().isOk());

        verify(fittingResultService).getFittingResults(1L, 25L, 10);
    }

    @Test
    @DisplayName("잘못된 목록 조회 조건은 400 Bad Request를 반환한다")
    void rejectInvalidPaginationParameter() throws Exception {
        given(fittingResultService.getFittingResults(1L, 0L, 20))
                .willThrow(new BusinessException(
                        ErrorCode.INVALID_PAGINATION_PARAMETER
                ));

        mockMvc.perform(get("/api/v1/fitting-results")
                        .queryParam("cursor", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_PAGINATION_PARAMETER"));
    }
}
