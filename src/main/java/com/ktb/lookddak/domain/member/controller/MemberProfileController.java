package com.ktb.lookddak.domain.member.controller;

import com.ktb.lookddak.domain.member.dto.MemberProfileCreateRequest;
import com.ktb.lookddak.domain.member.dto.MemberProfileCreateResponse;
import com.ktb.lookddak.domain.member.dto.MemberProfileGetResponse;
import com.ktb.lookddak.domain.member.service.MemberProfileService;
import com.ktb.lookddak.global.response.ApiResponse;
import com.ktb.lookddak.global.response.SuccessCode;
import com.ktb.lookddak.global.security.principal.MemberPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members/me/profile")
@RequiredArgsConstructor
public class MemberProfileController {

    private final MemberProfileService memberProfileService;

    @GetMapping
    public ResponseEntity<ApiResponse<MemberProfileGetResponse>> getProfile(
            @AuthenticationPrincipal MemberPrincipal principal
    ) {
        MemberProfileGetResponse response = memberProfileService.getProfile(
                principal.getMemberId()
        );

        return ResponseEntity
                .status(SuccessCode.OK.getStatus())
                .body(ApiResponse.success(SuccessCode.OK, response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MemberProfileCreateResponse>> createProfile(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody MemberProfileCreateRequest request
    ) {
        MemberProfileCreateResponse response = memberProfileService.createProfile(
                principal.getMemberId(),
                request
        );

        return ResponseEntity
                .status(SuccessCode.CREATED.getStatus())
                .body(ApiResponse.success(SuccessCode.CREATED, response));
    }
}
