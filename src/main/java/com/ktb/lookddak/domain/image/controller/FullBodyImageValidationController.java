package com.ktb.lookddak.domain.image.controller;

import com.ktb.lookddak.domain.image.dto.BodyImageWarmupResponse;
import com.ktb.lookddak.domain.image.dto.FullBodyImageValidationResponse;
import com.ktb.lookddak.domain.image.service.BodyImageWarmupService;
import com.ktb.lookddak.domain.image.service.FullBodyImageValidationService;
import com.ktb.lookddak.global.response.ApiResponse;
import com.ktb.lookddak.global.response.SuccessCode;
import com.ktb.lookddak.global.security.principal.MemberPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/full-body-image")
@RequiredArgsConstructor
public class FullBodyImageValidationController {

    private final FullBodyImageValidationService validationService;
    private final BodyImageWarmupService warmupService;

    @PostMapping("/warmup")
    public ResponseEntity<ApiResponse<Void>> warmup() {
        BodyImageWarmupResponse response = warmupService.requestWarmup();
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(response.status());
        if (response.retryAfterSeconds() != null) {
            builder.header(
                    HttpHeaders.RETRY_AFTER,
                    response.retryAfterSeconds().toString()
            );
        }

        if (response.status().is2xxSuccessful()) {
            return builder.body(ApiResponse.success(
                    SuccessCode.BODY_IMAGE_WARMUP_ACCEPTED,
                    null
            ));
        }
        return builder.body(ApiResponse.failure(
                response.code(),
                response.message()
        ));
    }

    @PostMapping(
            value = "/validate",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<FullBodyImageValidationResponse>>
            validate(
                    @AuthenticationPrincipal MemberPrincipal principal,
                    @RequestPart("image") MultipartFile image
            ) {
        FullBodyImageValidationResponse response =
                validationService.validate(
                        principal.getMemberId(),
                        image
                );

        return ResponseEntity
                .status(SuccessCode.OK.getStatus())
                .body(ApiResponse.success(SuccessCode.OK, response));
    }
}
