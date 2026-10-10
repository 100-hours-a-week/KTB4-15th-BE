package com.ktb.lookddak.domain.fitting.controller;

import com.ktb.lookddak.domain.fitting.dto.FittingResultDetailResponse;
import com.ktb.lookddak.domain.fitting.dto.FittingResultListResponse;
import com.ktb.lookddak.domain.fitting.dto.FittingResultOutfitNameUpdateResponse;
import com.ktb.lookddak.domain.fitting.entity.FittingJob;
import com.ktb.lookddak.domain.fitting.entity.FittingResult;
import com.ktb.lookddak.domain.fitting.service.FittingResultService;
import com.ktb.lookddak.domain.member.entity.Member;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

    @Test
    @DisplayName("저장된 가상피팅 결과 상세를 조회하면 200 OK를 반환한다")
    void getFittingResult() throws Exception {
        given(fittingResultService.getFittingResult(1L, 30L))
                .willReturn(createDetailResponse());

        mockMvc.perform(get("/api/v1/fitting-results/30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.fittingResultId").value(30))
                .andExpect(jsonPath("$.data.resultImageUrl")
                        .value("https://image.example.com/fittings/30.png"))
                .andExpect(jsonPath("$.data.outfitName").value("출근룩"))
                .andExpect(jsonPath("$.data.comment").value("AI 코디 설명"))
                .andExpect(jsonPath("$.data.products").isEmpty());

        verify(fittingResultService).getFittingResult(1L, 30L);
    }

    @Test
    @DisplayName("다른 회원의 가상피팅 결과는 403 Forbidden을 반환한다")
    void rejectOtherMembersFittingResult() throws Exception {
        given(fittingResultService.getFittingResult(1L, 30L))
                .willThrow(new BusinessException(
                        ErrorCode.FITTING_RESULT_ACCESS_DENIED
                ));

        mockMvc.perform(get("/api/v1/fitting-results/30"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code")
                        .value("FITTING_RESULT_ACCESS_DENIED"));
    }

    @Test
    @DisplayName("저장된 가상피팅 결과를 삭제하면 200 OK와 null 데이터를 반환한다")
    void deleteFittingResult() throws Exception {
        mockMvc.perform(delete("/api/v1/fitting-results/30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.message")
                        .value("요청이 성공적으로 처리되었습니다."));

        verify(fittingResultService).deleteFittingResult(1L, 30L);
    }

    @Test
    @DisplayName("다른 회원의 가상피팅 결과 삭제는 403 Forbidden을 반환한다")
    void rejectDeleteOtherMembersFittingResult() throws Exception {
        org.mockito.Mockito.doThrow(new BusinessException(
                ErrorCode.FITTING_RESULT_ACCESS_DENIED
        )).when(fittingResultService).deleteFittingResult(1L, 30L);

        mockMvc.perform(delete("/api/v1/fitting-results/30"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code")
                        .value("FITTING_RESULT_ACCESS_DENIED"));
    }

    @Test
    @DisplayName("저장된 가상피팅 결과의 코디명을 수정하면 200 OK를 반환한다")
    void updateOutfitName() throws Exception {
        given(fittingResultService.updateOutfitName(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(30L),
                any()
        )).willReturn(new FittingResultOutfitNameUpdateResponse(
                30L,
                "회사 데일리 니트 룩"
        ));

        mockMvc.perform(patch("/api/v1/fitting-results/30")
                        .contentType("application/json")
                        .content("""
                                {"outfitName": "회사 데일리 니트 룩"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.fittingResultId").value(30))
                .andExpect(jsonPath("$.data.outfitName")
                        .value("회사 데일리 니트 룩"));

        verify(fittingResultService).updateOutfitName(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(30L),
                any()
        );
    }

    @Test
    @DisplayName("잘못된 코디명 수정 요청은 400 Bad Request를 반환한다")
    void rejectInvalidOutfitNameUpdate() throws Exception {
        given(fittingResultService.updateOutfitName(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(30L),
                any()
        )).willThrow(new BusinessException(ErrorCode.INVALID_OUTFIT_NAME));

        mockMvc.perform(patch("/api/v1/fitting-results/30")
                        .contentType("application/json")
                        .content("""
                                {"outfitName": " "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_OUTFIT_NAME"));
    }

    private FittingResultDetailResponse createDetailResponse() {
        Member member = Member.create("result@lookddak.com", "password");
        ReflectionTestUtils.setField(member, "id", 1L);
        FittingJob fittingJob = FittingJob.create(member);
        ReflectionTestUtils.setField(fittingJob, "id", 10L);
        FittingResult fittingResult = FittingResult.create(
                fittingJob,
                "fittings/30.png",
                "출근룩",
                "AI 코디 설명"
        );
        ReflectionTestUtils.setField(fittingResult, "id", 30L);

        return FittingResultDetailResponse.from(
                fittingResult,
                "https://image.example.com/fittings/30.png",
                List.of()
        );
    }
}
