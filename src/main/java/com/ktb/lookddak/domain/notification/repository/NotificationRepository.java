package com.ktb.lookddak.domain.notification.repository;

import com.ktb.lookddak.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
