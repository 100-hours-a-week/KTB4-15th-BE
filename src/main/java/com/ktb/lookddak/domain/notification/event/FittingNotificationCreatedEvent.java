package com.ktb.lookddak.domain.notification.event;

import com.ktb.lookddak.domain.notification.entity.NotificationType;
import lombok.Getter;

@Getter
public class FittingNotificationCreatedEvent {

    private final Long notificationId;
    private final Long memberId;
    private final Long fittingJobId;
    private final NotificationType type;

    public FittingNotificationCreatedEvent(
            Long notificationId,
            Long memberId,
            Long fittingJobId,
            NotificationType type
    ) {
        this.notificationId = notificationId;
        this.memberId = memberId;
        this.fittingJobId = fittingJobId;
        this.type = type;
    }
}
