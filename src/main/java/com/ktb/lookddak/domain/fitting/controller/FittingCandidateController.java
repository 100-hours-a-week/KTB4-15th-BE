package com.ktb.lookddak.domain.fitting.controller;

import com.ktb.lookddak.domain.fitting.dto.FittingCandidateCreateRequest;
import com.ktb.lookddak.domain.fitting.dto.FittingCandidateCreateResponse;
import com.ktb.lookddak.domain.fitting.service.FittingCandidateService;
import com.ktb.lookddak.global.response.ApiResponse;
import com.ktb.lookddak.global.response.SuccessCode;
import com.ktb.lookddak.global.security.principal.MemberPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/fitting-candidates")
@RequiredArgsConstructor
public class FittingCandidateController {

    private final FittingCandidateService fittingCandidateService;

    @PostMapping
    public ResponseEntity<ApiResponse<FittingCandidateCreateResponse>>
            createFittingCandidate(
                    @AuthenticationPrincipal MemberPrincipal principal,
                    @Valid @RequestBody FittingCandidateCreateRequest request
            ) {
        FittingCandidateCreateResponse response =
                fittingCandidateService.createFittingCandidate(
                        principal.getMemberId(),
                        request
                );

        return ResponseEntity
                .status(SuccessCode.CREATED.getStatus())
                .body(ApiResponse.success(SuccessCode.CREATED, response));
    }

    @DeleteMapping("/{fittingCandidateId}")
    public ResponseEntity<ApiResponse<Void>> deleteFittingCandidate(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Positive @PathVariable Long fittingCandidateId
    ) {
        fittingCandidateService.deleteFittingCandidate(
                principal.getMemberId(),
                fittingCandidateId
        );

        return ResponseEntity
                .status(SuccessCode.OK.getStatus())
                .body(ApiResponse.success(SuccessCode.OK, null));
    }
}
