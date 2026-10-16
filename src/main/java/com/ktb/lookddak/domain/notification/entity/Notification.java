package com.ktb.lookddak.domain.notification.entity;

import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Entity
@Table(
        name = "notification",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notification_target",
                columnNames = {"target_type", "target_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    private static final int EXPIRATION_DAYS = 7;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "json")
    private Map<String, String> payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private NotificationTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    private Notification(
            Member member,
            NotificationType type,
            Map<String, String> payload,
            Long fittingJobId
    ) {
        this.member = member;
        this.type = type;
        this.payload = payload;
        this.targetType = NotificationTargetType.FITTING_JOB;
        this.targetId = fittingJobId;
        this.isRead = false;
        this.expiresAt = LocalDateTime.now().plusDays(EXPIRATION_DAYS);
    }

    public static Notification fittingSuccess(
            Member member,
            Long fittingJobId,
            String outfitName
    ) {
        return new Notification(
                member,
                NotificationType.FITTING_SUCCESS,
                Map.of("outfitName", outfitName),
                fittingJobId
        );
    }

    public static Notification fittingFailure(Member member, Long fittingJobId) {
        return new Notification(
                member,
                NotificationType.FITTING_FAIL,
                Map.of(),
                fittingJobId
        );
    }

    public boolean isOwnedBy(Long memberId) {
        return member.getId().equals(memberId);
    }

    public void markAsRead() {
        this.isRead = true;
    }
}
