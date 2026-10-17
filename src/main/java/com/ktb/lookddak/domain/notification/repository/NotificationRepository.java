package com.ktb.lookddak.domain.notification.repository;

import com.ktb.lookddak.domain.notification.entity.Notification;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select notification
            from Notification notification
            join fetch notification.member member
            where notification.id = :notificationId
            """)
    Optional<Notification> findByIdForReadUpdate(
            @Param("notificationId") Long notificationId
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Notification notification
            set notification.isRead = true
            where notification.member.id = :memberId
              and notification.isRead = false
            """)
    int markAllAsReadByMemberId(@Param("memberId") Long memberId);

    long countByMemberIdAndIsReadFalseAndExpiresAtAfter(
            Long memberId,
            LocalDateTime currentTime
    );

    boolean existsByMemberIdAndIsReadFalseAndExpiresAtAfter(
            Long memberId,
            LocalDateTime currentTime
    );

    List<Notification> findByMemberIdAndExpiresAtAfterOrderByCreatedAtDescIdDesc(
            Long memberId,
            LocalDateTime currentTime
    );
}
