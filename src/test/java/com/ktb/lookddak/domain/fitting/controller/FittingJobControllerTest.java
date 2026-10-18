package com.ktb.lookddak.domain.fitting.controller;

import com.ktb.lookddak.domain.fitting.dto.FittingJobCreateResponse;
import com.ktb.lookddak.domain.fitting.dto.FittingJobStatusResponse;
import com.ktb.lookddak.domain.fitting.dto.FittingJobResultViewedResponse;
import com.ktb.lookddak.domain.fitting.dto.FittingJobCurrentResponse;
import com.ktb.lookddak.domain.fitting.dto.FittingTempResultDetailResponse;
import com.ktb.lookddak.domain.fitting.entity.FittingJob;
import com.ktb.lookddak.domain.fitting.entity.FittingTempResult;
import com.ktb.lookddak.domain.fitting.service.FittingJobService;
import com.ktb.lookddak.domain.fitting.service.FittingJobResultViewService;
import com.ktb.lookddak.domain.fitting.service.FittingJobCurrentService;
import com.ktb.lookddak.domain.fitting.service.FittingTempResultQueryService;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FittingJobControllerTest {

    @Mock
    private FittingJobService fittingJobService;

    @Mock
    private FittingJobResultViewService fittingJobResultViewService;

    @Mock
    private FittingJobCurrentService fittingJobCurrentService;

    @Mock
    private FittingTempResultQueryService fittingTempResultQueryService;

    @Mock
    private MemberPrincipal principal;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new FittingJobController(
                        fittingJobService,
                        fittingJobResultViewService,
                        fittingJobCurrentService,
                        fittingTempResultQueryService
                ))
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
    @DisplayName("가상피팅 작업을 생성하면 201 Created를 반환한다")
    void createFittingJob() throws Exception {
        FittingJob fittingJob = createFittingJob(100L);
        given(fittingJobService.createFittingJob(eq(1L), any()))
                .willReturn(FittingJobCreateResponse.from(fittingJob));

        mockMvc.perform(post("/api/v1/fitting-jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topProductId": 10,
                                  "bottomProductId": 20
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CREATED"))
                .andExpect(jsonPath("$.data.fittingJobId").value(100))
                .andExpect(jsonPath("$.data.status")
                        .value("GENERATING"))
                .andExpect(jsonPath("$.message")
                        .value("리소스가 성공적으로 생성되었습니다."));

        verify(fittingJobService).createFittingJob(eq(1L), any());
    }

    @Test
    @DisplayName("상품 ID가 양수가 아니면 400 Bad Request를 반환한다")
    void rejectNonPositiveProductId() throws Exception {
        mockMvc.perform(post("/api/v1/fitting-jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topProductId": 0,
                                  "bottomProductId": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.data").isEmpty());

        verifyNoInteractions(fittingJobService);
    }

    @Test
    @DisplayName("생성 중인 작업이 있으면 409 Conflict를 반환한다")
    void rejectDuplicatedGeneratingJob() throws Exception {
        given(fittingJobService.createFittingJob(eq(1L), any()))
                .willThrow(new BusinessException(
                        ErrorCode.FITTING_JOB_ALREADY_GENERATING
                ));

        mockMvc.perform(post("/api/v1/fitting-jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"topProductId": 10}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("FITTING_JOB_ALREADY_GENERATING"))
                .andExpect(jsonPath("$.message")
                        .value("이미 생성 중인 가상피팅 작업이 있습니다."));
    }

    @Test
    @DisplayName("가상피팅 요청 횟수를 모두 사용하면 429 Too Many Requests를 반환한다")
    void rejectFittingRequestLimit() throws Exception {
        given(fittingJobService.createFittingJob(eq(1L), any()))
                .willThrow(new BusinessException(
                        ErrorCode.FITTING_REQUEST_LIMIT_EXCEEDED
                ));

        mockMvc.perform(post("/api/v1/fitting-jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"topProductId": 10}
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code")
                        .value("FITTING_REQUEST_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.message")
                        .value("가상피팅은 최대 10회까지 요청할 수 있습니다."));
    }

    @Test
    @DisplayName("생성 중인 가상피팅 작업 상태를 조회한다")
    void getFittingJobStatus() throws Exception {
        FittingJob fittingJob = createFittingJob(100L);
        given(fittingJobService.getFittingJobStatus(1L, 100L))
                .willReturn(FittingJobStatusResponse.from(
                        fittingJob,
                        null
                ));

        mockMvc.perform(get("/api/v1/fitting-jobs/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.fittingJobId").value(100))
                .andExpect(jsonPath("$.data.status")
                        .value("GENERATING"))
                .andExpect(jsonPath("$.data.result").isEmpty())
                .andExpect(jsonPath("$.message")
                        .value("요청이 성공적으로 처리되었습니다."));

        verify(fittingJobService).getFittingJobStatus(1L, 100L);
    }

    @Test
    @DisplayName("작업 ID가 숫자가 아니면 400 Bad Request를 반환한다")
    void rejectNonNumericFittingJobId() throws Exception {
        mockMvc.perform(get("/api/v1/fitting-jobs/invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_INPUT_VALUE"));

        verifyNoInteractions(fittingJobService);
    }

    @Test
    @DisplayName("다른 회원의 작업은 403 Forbidden을 반환한다")
    void rejectOtherMembersFittingJob() throws Exception {
        given(fittingJobService.getFittingJobStatus(1L, 100L))
                .willThrow(new BusinessException(
                        ErrorCode.FITTING_JOB_ACCESS_DENIED
                ));

        mockMvc.perform(get("/api/v1/fitting-jobs/100"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code")
                        .value("FITTING_JOB_ACCESS_DENIED"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("가상피팅 결과 확인 처리를 한다")
    void markFittingJobResultViewed() throws Exception {
        FittingJob fittingJob = createFittingJob(100L);
        given(fittingJobResultViewService.markResultViewed(1L, 100L))
                .willReturn(FittingJobResultViewedResponse.from(fittingJob));

        mockMvc.perform(patch(
                        "/api/v1/fitting-jobs/100/result/viewed"
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.fittingJobId").value(100))
                .andExpect(jsonPath("$.message")
                        .value("요청이 성공적으로 처리되었습니다."));

        verify(fittingJobResultViewService).markResultViewed(1L, 100L);
    }

    @Test
    @DisplayName("최근 가상피팅 작업을 조회한다")
    void getCurrentFittingJob() throws Exception {
        FittingJob fittingJob = createFittingJob(100L);
        given(fittingJobCurrentService.getCurrentFittingJob(1L))
                .willReturn(FittingJobCurrentResponse.from(
                        fittingJob,
                        List.of()
                ));

        mockMvc.perform(get("/api/v1/fitting-jobs/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.fittingJobId").value(100))
                .andExpect(jsonPath("$.data.status").value("GENERATING"))
                .andExpect(jsonPath("$.data.resultViewed").value(false))
                .andExpect(jsonPath("$.data.products").isEmpty())
                .andExpect(jsonPath("$.message")
                        .value("요청이 성공적으로 처리되었습니다."));
    }

    @Test
    @DisplayName("확인할 최근 가상피팅 작업이 없으면 null을 반환한다")
    void returnNullWhenNoCurrentFittingJob() throws Exception {
        given(fittingJobCurrentService.getCurrentFittingJob(1L))
                .willReturn(null);

        mockMvc.perform(get("/api/v1/fitting-jobs/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.message")
                        .value("요청이 성공적으로 처리되었습니다."));
    }

    @Test
    @DisplayName("완료된 가상피팅 임시 결과를 조회한다")
    void getFittingTempResult() throws Exception {
        FittingJob fittingJob = createFittingJob(100L);
        given(fittingTempResultQueryService.getTempResult(1L, 100L))
                .willReturn(tempResultResponse(fittingJob));

        mockMvc.perform(get("/api/v1/fitting-jobs/100/result"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.fittingJobId").value(100))
                .andExpect(jsonPath("$.data.saved").value(false))
                .andExpect(jsonPath("$.data.fittingResultId").isEmpty())
                .andExpect(jsonPath("$.data.resultImageUrl")
                        .value("https://image.example.com/result.jpg"))
                .andExpect(jsonPath("$.message")
                        .value("요청이 성공적으로 처리되었습니다."));
    }

    private FittingJob createFittingJob(Long fittingJobId) {
        Member member = Member.create(
                "fitting-controller@lookddak.com",
                "encoded-password"
        );
        ReflectionTestUtils.setField(member, "id", 1L);

        FittingJob fittingJob = FittingJob.create(member);
        ReflectionTestUtils.setField(fittingJob, "id", fittingJobId);
        return fittingJob;
    }

    private FittingTempResultDetailResponse tempResultResponse(
            FittingJob fittingJob
    ) {
        FittingTempResult tempResult = FittingTempResult.create(
                fittingJob,
                "fittings/result.jpg",
                "출근 코디",
                "코디 설명"
        );
        return FittingTempResultDetailResponse.from(
                fittingJob,
                tempResult,
                null,
                "https://image.example.com/result.jpg",
                List.of()
        );
    }
}
