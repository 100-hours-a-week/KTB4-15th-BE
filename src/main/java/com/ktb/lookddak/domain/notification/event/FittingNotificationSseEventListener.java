package com.ktb.lookddak.domain.notification.event;

import com.ktb.lookddak.domain.notification.service.NotificationSseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class FittingNotificationSseEventListener {

    private final NotificationSseService notificationSseService;

    /**
     * DB 커밋이 성공한 뒤에만 SSE 전송을 시도한다.
     * 이 리스너는 DB를 조회하거나 새 트랜잭션을 시작하지 않는다.
     */
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true
    )
    public void handle(FittingNotificationCreatedEvent event) {
        notificationSseService.sendNotification(
                event.getMemberId(),
                event.getNotificationId(),
                event.getFittingJobId(),
                event.getType()
        );
    }
}
