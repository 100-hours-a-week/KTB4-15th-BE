package com.ktb.lookddak.domain.fitting.controller;

import com.ktb.lookddak.domain.fitting.dto.FittingJobCreateRequest;
import com.ktb.lookddak.domain.fitting.dto.FittingJobCreateResponse;
import com.ktb.lookddak.domain.fitting.dto.FittingJobStatusResponse;
import com.ktb.lookddak.domain.fitting.dto.FittingJobResultViewedResponse;
import com.ktb.lookddak.domain.fitting.service.FittingJobService;
import com.ktb.lookddak.domain.fitting.service.FittingJobResultViewService;
import com.ktb.lookddak.global.response.ApiResponse;
import com.ktb.lookddak.global.response.SuccessCode;
import com.ktb.lookddak.global.security.principal.MemberPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/fitting-jobs")
@RequiredArgsConstructor
public class FittingJobController {

    private final FittingJobService fittingJobService;
    private final FittingJobResultViewService fittingJobResultViewService;

    @PostMapping
    public ResponseEntity<ApiResponse<FittingJobCreateResponse>>
            createFittingJob(
                    @AuthenticationPrincipal MemberPrincipal principal,
                    @Valid @RequestBody FittingJobCreateRequest request
            ) {
        FittingJobCreateResponse response =
                fittingJobService.createFittingJob(
                        principal.getMemberId(),
                        request
                );

        return ResponseEntity
                .status(SuccessCode.CREATED.getStatus())
                .body(ApiResponse.success(SuccessCode.CREATED, response));
    }

    @GetMapping("/{fittingJobId}")
    public ResponseEntity<ApiResponse<FittingJobStatusResponse>>
            getFittingJobStatus(
                    @AuthenticationPrincipal MemberPrincipal principal,
                    @Positive @PathVariable Long fittingJobId
            ) {
        FittingJobStatusResponse response =
                fittingJobService.getFittingJobStatus(
                        principal.getMemberId(),
                        fittingJobId
                );

        return ResponseEntity
                .status(SuccessCode.OK.getStatus())
                .body(ApiResponse.success(SuccessCode.OK, response));
    }

    @PatchMapping("/{fittingJobId}/result/viewed")
    public ResponseEntity<ApiResponse<FittingJobResultViewedResponse>>
            markFittingJobResultViewed(
                    @AuthenticationPrincipal MemberPrincipal principal,
                    @Positive @PathVariable Long fittingJobId
            ) {
        FittingJobResultViewedResponse response =
                fittingJobResultViewService.markResultViewed(
                        principal.getMemberId(),
                        fittingJobId
                );

        return ResponseEntity
                .status(SuccessCode.OK.getStatus())
                .body(ApiResponse.success(
                        SuccessCode.OK,
                        response,
                        "가상피팅 결과를 확인 처리했습니다."
                ));
    }
}
