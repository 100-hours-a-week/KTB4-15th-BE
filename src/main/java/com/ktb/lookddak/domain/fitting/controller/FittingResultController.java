package com.ktb.lookddak.domain.fitting.controller;

import com.ktb.lookddak.domain.fitting.dto.FittingResultCreateRequest;
import com.ktb.lookddak.domain.fitting.dto.FittingResultCreateResponse;
import com.ktb.lookddak.domain.fitting.service.FittingResultService;
import com.ktb.lookddak.global.response.ApiResponse;
import com.ktb.lookddak.global.response.SuccessCode;
import com.ktb.lookddak.global.security.principal.MemberPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fitting-results")
@RequiredArgsConstructor
public class FittingResultController {

    private final FittingResultService fittingResultService;

    @PostMapping
    public ResponseEntity<ApiResponse<FittingResultCreateResponse>>
            createFittingResult(
                    @AuthenticationPrincipal MemberPrincipal principal,
                    @Valid @RequestBody FittingResultCreateRequest request
            ) {
        FittingResultCreateResponse response =
                fittingResultService.createFittingResult(
                        principal.getMemberId(),
                        request
                );

        return ResponseEntity
                .status(SuccessCode.CREATED.getStatus())
                .body(ApiResponse.success(SuccessCode.CREATED, response));
    }
}
