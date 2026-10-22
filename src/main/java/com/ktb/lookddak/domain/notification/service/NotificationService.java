package com.ktb.lookddak.domain.notification.service;

import com.ktb.lookddak.domain.notification.dto.NotificationReadResponse;
import com.ktb.lookddak.domain.notification.dto.NotificationListItemResponse;
import com.ktb.lookddak.domain.notification.dto.NotificationListResponse;
import com.ktb.lookddak.domain.notification.dto.NotificationUnreadStatusResponse;
import com.ktb.lookddak.domain.notification.entity.Notification;
import com.ktb.lookddak.domain.notification.repository.NotificationRepository;
import com.ktb.lookddak.global.exception.BusinessException;
import com.ktb.lookddak.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    public NotificationUnreadStatusResponse getUnreadStatus(Long memberId) {
        boolean hasUnread = notificationRepository
                .existsByMemberIdAndIsReadFalseAndExpiresAtAfter(
                        memberId,
                        LocalDateTime.now()
                );
        return new NotificationUnreadStatusResponse(hasUnread);
    }

    public NotificationListResponse getNotifications(Long memberId) {
        List<Notification> notifications = notificationRepository
                .findByMemberIdAndExpiresAtAfterOrderByCreatedAtDescIdDesc(
                        memberId,
                        LocalDateTime.now()
                );

        List<NotificationListItemResponse> items = new ArrayList<>();
        for (Notification notification : notifications) {
            items.add(NotificationListItemResponse.from(notification));
        }
        long unreadCount = items.stream()
                .filter(item -> !item.isRead())
                .count();

        return new NotificationListResponse(items, unreadCount);
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
