package com.ktb.lookddak.domain.fitting.controller;

import com.ktb.lookddak.domain.fitting.dto.FittingJobCreateRequest;
import com.ktb.lookddak.domain.fitting.dto.FittingJobCreateResponse;
import com.ktb.lookddak.domain.fitting.dto.FittingJobStatusResponse;
import com.ktb.lookddak.domain.fitting.service.FittingJobService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/fitting-jobs")
@RequiredArgsConstructor
public class FittingJobController {

    private final FittingJobService fittingJobService;

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
}
