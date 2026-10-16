package com.ktb.lookddak.domain.notification.dto;

public record NotificationSseConnectedResponse(
        long unreadCount,
        boolean hasUnread
) {
}
