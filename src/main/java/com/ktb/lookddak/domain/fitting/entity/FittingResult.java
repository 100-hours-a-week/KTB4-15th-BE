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

/**
 * 사용자가 보관하기로 선택한 가상피팅 결과다.
 * AI가 만든 원본 결과는 FittingTempResult에 유지하고, 최종 코디명과 결과 정보를 별도로 보관한다.
 */
@Getter
@Entity
@Table(
        name = "fitting_result",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_fitting_result_job",
                columnNames = "fitting_job_id"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FittingResult extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fitting_job_id", nullable = false, unique = true)
    private FittingJob fittingJob;

    @Column(name = "result_image_key", nullable = false, length = 2048)
    private String resultImageKey;

    @Column(name = "outfit_name", nullable = false, length = 20)
    private String outfitName;

    @Column(name = "ai_comment", nullable = false, columnDefinition = "TEXT")
    private String aiComment;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    private FittingResult(
            FittingJob fittingJob,
            String resultImageKey,
            String outfitName,
            String aiComment
    ) {
        this.fittingJob = fittingJob;
        this.resultImageKey = resultImageKey;
        this.outfitName = outfitName;
        this.aiComment = aiComment;
    }

    public static FittingResult create(
            FittingJob fittingJob,
            String resultImageKey,
            String outfitName,
            String aiComment
    ) {
        return new FittingResult(
                fittingJob,
                resultImageKey,
                outfitName,
                aiComment
        );
    }

    public boolean isOwnedBy(Long memberId) {
        return fittingJob.isOwnedBy(memberId);
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void updateOutfitName(String outfitName) {
        this.outfitName = outfitName;
    }

    public void delete() {
        if (deletedAt == null) {
            deletedAt = LocalDateTime.now();
        }
    }

    public void restore(String outfitName) {
        deletedAt = null;
        updateOutfitName(outfitName);
    }
}
