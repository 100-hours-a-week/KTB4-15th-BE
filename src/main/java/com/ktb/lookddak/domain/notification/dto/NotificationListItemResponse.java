package com.ktb.lookddak.domain.notification.dto;

import com.ktb.lookddak.domain.notification.entity.Notification;
import com.ktb.lookddak.domain.notification.entity.NotificationTargetType;
import com.ktb.lookddak.domain.notification.entity.NotificationType;

import java.time.LocalDateTime;
import java.util.Map;

public record NotificationListItemResponse(
        Long notificationId,
        NotificationType type,
        boolean isRead,
        NotificationTargetType targetType,
        Long targetId,
        Map<String, String> payload,
        LocalDateTime createdAt
) {

    public static NotificationListItemResponse from(Notification notification) {
        return new NotificationListItemResponse(
                notification.getId(),
                notification.getType(),
                notification.isRead(),
                notification.getTargetType(),
                notification.getTargetId(),
                Map.copyOf(notification.getPayload()),
                notification.getCreatedAt()
        );
    }
}
