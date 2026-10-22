package com.ktb.lookddak.domain.notification.dto;

import java.util.List;

public record NotificationListResponse(
        List<NotificationListItemResponse> items,
        long unreadCount
) {
}
