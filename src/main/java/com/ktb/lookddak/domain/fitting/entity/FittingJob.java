package com.ktb.lookddak.domain.fitting.entity;

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
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "fitting_job",
        indexes = @Index(
                name = "idx_fitting_job_member_status",
                columnList = "member_id,status"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FittingJob extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FittingJobStatus status;

    private FittingJob(Member member) {
        this.member = member;
        this.status = FittingJobStatus.GENERATING;
    }

    public static FittingJob create(Member member) {
        return new FittingJob(member);
    }

    public boolean isOwnedBy(Long memberId) {
        return member.getId().equals(memberId);
    }

    public void completeGeneration() {
        updateStatus(FittingJobStatus.COMPLETED);
    }

    public void failGeneration() {
        updateStatus(FittingJobStatus.FAILED);
    }

    private void updateStatus(FittingJobStatus status) {
        if (this.status != FittingJobStatus.GENERATING) {
            throw new IllegalStateException(
                    "생성 중인 가상피팅 작업만 상태를 변경할 수 있습니다."
            );
        }

        this.status = status;
    }
}
