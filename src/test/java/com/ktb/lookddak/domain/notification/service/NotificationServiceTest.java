package com.ktb.lookddak.domain.notification.service;

import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.notification.dto.NotificationReadResponse;
import com.ktb.lookddak.domain.notification.dto.NotificationListResponse;
import com.ktb.lookddak.domain.notification.dto.NotificationUnreadStatusResponse;
import com.ktb.lookddak.domain.notification.entity.Notification;
import com.ktb.lookddak.domain.notification.entity.NotificationTargetType;
import com.ktb.lookddak.domain.notification.entity.NotificationType;
import com.ktb.lookddak.domain.notification.repository.NotificationRepository;
import com.ktb.lookddak.global.exception.BusinessException;
import com.ktb.lookddak.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("본인 알림을 읽음 처리하고 남은 읽지 않은 알림 수를 반환한다")
    void markAsRead() {
        Notification notification = fittingSuccessNotification(1L);
        given(notificationRepository.findByIdForReadUpdate(100L))
                .willReturn(Optional.of(notification));
        given(notificationRepository.countByMemberIdAndIsReadFalseAndExpiresAtAfter(
                org.mockito.ArgumentMatchers.eq(1L),
                any(LocalDateTime.class)
        )).willReturn(2L);

        NotificationReadResponse response = notificationService.markAsRead(1L, 100L);

        assertThat(notification.isRead()).isTrue();
        assertThat(response.unreadCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("이미 읽은 알림도 다시 읽음 처리할 수 있다")
    void markAsReadIdempotently() {
        Notification notification = fittingSuccessNotification(1L);
        notification.markAsRead();
        given(notificationRepository.findByIdForReadUpdate(100L))
                .willReturn(Optional.of(notification));
        given(notificationRepository.countByMemberIdAndIsReadFalseAndExpiresAtAfter(
                org.mockito.ArgumentMatchers.eq(1L),
                any(LocalDateTime.class)
        )).willReturn(0L);

        NotificationReadResponse response = notificationService.markAsRead(1L, 100L);

        assertThat(response.unreadCount()).isZero();
    }

    @Test
    @DisplayName("다른 회원의 알림은 읽음 처리할 수 없다")
    void rejectsOtherMembersNotification() {
        Notification notification = fittingSuccessNotification(2L);
        given(notificationRepository.findByIdForReadUpdate(100L))
                .willReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.markAsRead(1L, 100L))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.NOTIFICATION_ACCESS_DENIED);
    }

    @Test
    @DisplayName("현재 회원의 읽지 않은 알림을 모두 읽음 처리한다")
    void markAllAsRead() {
        given(notificationRepository.countByMemberIdAndIsReadFalseAndExpiresAtAfter(
                org.mockito.ArgumentMatchers.eq(1L),
                any(LocalDateTime.class)
        )).willReturn(0L);

        NotificationReadResponse response = notificationService.markAllAsRead(1L);

        then(notificationRepository).should().markAllAsReadByMemberId(1L);
        assertThat(response.unreadCount()).isZero();
    }

    @Test
    @DisplayName("만료되지 않은 읽지 않은 알림이 있으면 존재 여부를 반환한다")
    void getUnreadStatus() {
        given(notificationRepository
                .existsByMemberIdAndIsReadFalseAndExpiresAtAfter(
                        org.mockito.ArgumentMatchers.eq(1L),
                        any(LocalDateTime.class)
                ))
                .willReturn(true);

        NotificationUnreadStatusResponse response = notificationService
                .getUnreadStatus(1L);

        assertThat(response.hasUnread()).isTrue();
    }

    @Test
    @DisplayName("만료되지 않은 본인 알림을 저장소가 반환한 최신순으로 조회한다")
    void getNotifications() {
        Notification newest = mock(Notification.class);
        given(newest.getId()).willReturn(20L);
        given(newest.getType()).willReturn(NotificationType.FITTING_SUCCESS);
        given(newest.isRead()).willReturn(false);
        given(newest.getTargetType()).willReturn(NotificationTargetType.FITTING_JOB);
        given(newest.getTargetId()).willReturn(10L);
        given(newest.getPayload()).willReturn(Map.of("outfitName", "가을 출근 니트 룩"));
        given(newest.getCreatedAt()).willReturn(LocalDateTime.of(2026, 10, 17, 12, 0));

        Notification older = mock(Notification.class);
        given(older.getId()).willReturn(19L);
        given(older.getType()).willReturn(NotificationType.FITTING_FAIL);
        given(older.isRead()).willReturn(true);
        given(older.getTargetType()).willReturn(NotificationTargetType.FITTING_JOB);
        given(older.getTargetId()).willReturn(9L);
        given(older.getPayload()).willReturn(Map.of());
        given(older.getCreatedAt()).willReturn(LocalDateTime.of(2026, 10, 17, 11, 0));

        given(notificationRepository
                .findByMemberIdAndExpiresAtAfterOrderByCreatedAtDescIdDesc(
                        org.mockito.ArgumentMatchers.eq(1L),
                        any(LocalDateTime.class)
                ))
                .willReturn(List.of(newest, older));

        NotificationListResponse response = notificationService.getNotifications(1L);

        assertThat(response.items()).hasSize(2);
        assertThat(response.items().getFirst().notificationId()).isEqualTo(20L);
        assertThat(response.items().getFirst().payload())
                .containsEntry("outfitName", "가을 출근 니트 룩");
        assertThat(response.items().get(1).payload()).isEmpty();
        assertThat(response.unreadCount()).isEqualTo(1L);
    }

    private Notification fittingSuccessNotification(Long memberId) {
        Member member = mock(Member.class);
        given(member.getId()).willReturn(memberId);
        return Notification.fittingSuccess(member, 10L, "코디명");
    }
}
