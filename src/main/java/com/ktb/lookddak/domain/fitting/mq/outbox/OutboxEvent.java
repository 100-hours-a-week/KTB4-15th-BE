package com.ktb.lookddak.domain.fitting.mq.outbox;

import com.ktb.lookddak.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "outbox_event",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_outbox_event_type_aggregate",
                columnNames = {"event_type", "aggregate_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private OutboxEventType eventType;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxEventStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "next_retry_at", nullable = false)
    private LocalDateTime nextRetryAt;

    @Column(name = "claimed_by", length = 100)
    private String claimedBy;

    @Column(name = "lease_until")
    private LocalDateTime leaseUntil;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    private OutboxEvent(
            OutboxEventType eventType,
            Long aggregateId,
            String payload,
            LocalDateTime createdAt
    ) {
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.status = OutboxEventStatus.PENDING;
        this.retryCount = 0;
        this.nextRetryAt = createdAt;
    }

    public static OutboxEvent fittingRequest(
            Long fittingJobId,
            String payload,
            LocalDateTime createdAt
    ) {
        return new OutboxEvent(
                OutboxEventType.FITTING_REQUEST,
                fittingJobId,
                payload,
                createdAt
        );
    }

    public boolean isClaimableAt(LocalDateTime now) {
        if (status == OutboxEventStatus.PENDING) {
            return !nextRetryAt.isAfter(now);
        }

        return status == OutboxEventStatus.PROCESSING
                && leaseUntil != null
                && !leaseUntil.isAfter(now);
    }

    public void claim(
            String serverId,
            LocalDateTime now,
            LocalDateTime leaseUntil
    ) {
        if (!isClaimableAt(now)) {
            throw new IllegalStateException("현재 Claim할 수 없는 Outbox 이벤트입니다.");
        }

        this.status = OutboxEventStatus.PROCESSING;
        this.claimedBy = serverId;
        this.leaseUntil = leaseUntil;
    }

    public void markPublished(LocalDateTime publishedAt) {
        assertProcessing();

        this.status = OutboxEventStatus.PUBLISHED;
        this.publishedAt = publishedAt;
        this.claimedBy = null;
        this.leaseUntil = null;
        this.lastError = null;
    }

    public void markPublishFailed(
            LocalDateTime nextRetryAt,
            String lastError
    ) {
        assertProcessing();

        this.status = OutboxEventStatus.PENDING;
        this.retryCount++;
        this.nextRetryAt = nextRetryAt;
        this.claimedBy = null;
        this.leaseUntil = null;
        this.lastError = lastError;
    }

    private void assertProcessing() {
        if (status != OutboxEventStatus.PROCESSING) {
            throw new IllegalStateException("처리 중인 Outbox 이벤트만 발행 결과를 반영할 수 있습니다.");
        }
    }
}
