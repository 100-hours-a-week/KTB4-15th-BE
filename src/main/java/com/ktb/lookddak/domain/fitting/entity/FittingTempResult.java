package com.ktb.lookddak.domain.fitting.entity;

import com.ktb.lookddak.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "fitting_temp_result",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_fitting_temp_result_job",
                columnNames = "fitting_job_id"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FittingTempResult extends BaseEntity {

    private static final long VALID_DAYS = 7L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fitting_job_id", nullable = false, unique = true)
    private FittingJob fittingJob;

    @Column(name = "result_image_key", nullable = false, length = 2048)
    private String resultImageKey;

    @Column(name = "outfit_name", nullable = false, length = 100)
    private String outfitName;

    @Column(name = "ai_comment", nullable = false, columnDefinition = "TEXT")
    private String aiComment;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    private FittingTempResult(
            FittingJob fittingJob,
            String resultImageKey,
            String outfitName,
            String aiComment
    ) {
        this.fittingJob = fittingJob;
        this.resultImageKey = resultImageKey;
        this.outfitName = outfitName;
        this.aiComment = aiComment;
        this.expiresAt = LocalDateTime.now().plusDays(VALID_DAYS);
    }

    public static FittingTempResult create(
            FittingJob fittingJob,
            String resultImageKey,
            String outfitName,
            String aiComment
    ) {
        return new FittingTempResult(
                fittingJob,
                resultImageKey,
                outfitName,
                aiComment
        );
    }

    public boolean isExpired(LocalDateTime now) {
        return !expiresAt.isAfter(now);
    }
}
