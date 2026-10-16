package com.ktb.lookddak.domain.notification.service;

import com.ktb.lookddak.domain.notification.dto.NotificationReadResponse;
import com.ktb.lookddak.domain.notification.entity.Notification;
import com.ktb.lookddak.domain.notification.repository.NotificationRepository;
import com.ktb.lookddak.global.exception.BusinessException;
import com.ktb.lookddak.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public long getUnreadCount(Long memberId) {
        return notificationRepository
                .countByMemberIdAndIsReadFalseAndExpiresAtAfter(
                        memberId,
                        LocalDateTime.now()
                );
    }

    @Transactional
    public NotificationReadResponse markAsRead(
            Long memberId,
            Long notificationId
    ) {
        Notification notification = notificationRepository
                .findByIdForReadUpdate(notificationId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOTIFICATION_NOT_FOUND
                ));

        if (!notification.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.NOTIFICATION_ACCESS_DENIED);
        }

        notification.markAsRead();
        return createUnreadCountResponse(memberId);
    }

    @Transactional
    public NotificationReadResponse markAllAsRead(Long memberId) {
        notificationRepository.markAllAsReadByMemberId(memberId);
        return createUnreadCountResponse(memberId);
    }

    private NotificationReadResponse createUnreadCountResponse(Long memberId) {
        return new NotificationReadResponse(getUnreadCount(memberId));
    }
}
