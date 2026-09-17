package com.ktb.lookddak.domain.member.entity;

import com.ktb.lookddak.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "member",
        uniqueConstraints = @UniqueConstraint(name = "uk_member_email", columnNames = "email")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 60)
    private String passwordHash;

    @Column(name = "price_alert_enabled", nullable = false)
    @ColumnDefault("false")
    private boolean priceAlertEnabled;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    private Member(String email, String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.priceAlertEnabled = false;
    }

    public static Member create(String email, String passwordHash) {
        return new Member(email, passwordHash);
    }
}
