package com.ktb.lookddak.domain.notification.controller;

import com.ktb.lookddak.domain.notification.service.NotificationSseService;
import com.ktb.lookddak.domain.notification.service.NotificationService;
import com.ktb.lookddak.domain.notification.dto.NotificationReadRequest;
import com.ktb.lookddak.domain.notification.dto.NotificationReadResponse;
import com.ktb.lookddak.global.response.ApiResponse;
import com.ktb.lookddak.global.response.SuccessCode;
import com.ktb.lookddak.global.security.principal.MemberPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@Validated
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationSseService notificationSseService;
    private final NotificationService notificationService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connectStream(
            @AuthenticationPrincipal MemberPrincipal principal
    ) {
        return notificationSseService.connect(principal.getMemberId());
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationReadResponse>> markAsRead(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Positive @PathVariable Long notificationId,
            @Valid @RequestBody NotificationReadRequest request
    ) {
        NotificationReadResponse response = notificationService.markAsRead(
                principal.getMemberId(),
                notificationId
        );

        return ResponseEntity
                .status(SuccessCode.OK.getStatus())
                .body(ApiResponse.success(SuccessCode.OK, response));
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<NotificationReadResponse>> markAllAsRead(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody NotificationReadRequest request
    ) {
        NotificationReadResponse response = notificationService.markAllAsRead(
                principal.getMemberId()
        );

        return ResponseEntity
                .status(SuccessCode.OK.getStatus())
                .body(ApiResponse.success(SuccessCode.OK, response));
    }
}
