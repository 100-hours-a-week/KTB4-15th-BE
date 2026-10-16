package com.ktb.lookddak.domain.notification.service;

import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.notification.dto.NotificationReadResponse;
import com.ktb.lookddak.domain.notification.entity.Notification;
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

    private Notification fittingSuccessNotification(Long memberId) {
        Member member = mock(Member.class);
        given(member.getId()).willReturn(memberId);
        return Notification.fittingSuccess(member, 10L);
    }
}
