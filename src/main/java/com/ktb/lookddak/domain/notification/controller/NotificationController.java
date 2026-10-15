package com.ktb.lookddak.domain.notification.controller;

import com.ktb.lookddak.domain.notification.service.NotificationSseService;
import com.ktb.lookddak.global.security.principal.MemberPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationSseService notificationSseService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connectStream(
            @AuthenticationPrincipal MemberPrincipal principal
    ) {
        return notificationSseService.connect(principal.getMemberId());
    }
}
