package com.ktb.lookddak.domain.fitting.controller;

import com.ktb.lookddak.domain.fitting.dto.FittingCandidateCreateResponse;
import com.ktb.lookddak.domain.fitting.service.FittingCandidateService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
}
