package com.ktb.lookddak.domain.notification.dto;

import com.ktb.lookddak.domain.notification.entity.NotificationType;

public record NotificationSseResponse(
        Long notificationId,
        Long fittingJobId,
        NotificationType type
) {
}
