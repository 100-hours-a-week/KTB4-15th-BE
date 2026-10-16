package com.ktb.lookddak.domain.notification.dto;

public record NotificationSseEventResponse<T>(
        String code,
        T data,
        String message
) {

    public static NotificationSseEventResponse<NotificationSseConnectedResponse>
            connected(long unreadCount) {
        return new NotificationSseEventResponse<>(
                "NOTIFICATION_STREAM_CONNECTED",
                new NotificationSseConnectedResponse(unreadCount, unreadCount > 0),
                "실시간 알림 연결이 완료되었습니다."
        );
    }

    public static NotificationSseEventResponse<NotificationSseResponse> received(
            NotificationSseResponse data
    ) {
        return new NotificationSseEventResponse<>(
                "NOTIFICATION_RECEIVED",
                data,
                "새 알림이 도착했습니다."
        );
    }
}
