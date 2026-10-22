package com.ktb.lookddak.domain.fitting.repository;

import com.ktb.lookddak.domain.fitting.entity.FittingJob;
import com.ktb.lookddak.domain.fitting.entity.FittingJobStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;

public interface FittingJobRepository extends JpaRepository<FittingJob, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select fittingJob
            from FittingJob fittingJob
            where fittingJob.id = :fittingJobId
            """)
    Optional<FittingJob> findByIdForGenerationUpdate(
            @Param("fittingJobId") Long fittingJobId
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update FittingJob fittingJob
            set fittingJob.status = :targetStatus
            where fittingJob.id = :fittingJobId
              and fittingJob.status = :currentStatus
            """)
    int updateStatusIfCurrent(
            @Param("fittingJobId") Long fittingJobId,
            @Param("currentStatus") FittingJobStatus currentStatus,
            @Param("targetStatus") FittingJobStatus targetStatus
    );

    boolean existsByMemberIdAndStatus(
            Long memberId,
            FittingJobStatus status
    );

    @Query("""
            select fittingJob
            from FittingJob fittingJob
            join fetch fittingJob.member member
            where fittingJob.id = :fittingJobId
            """)
    Optional<FittingJob> findByIdWithMember(
            @Param("fittingJobId") Long fittingJobId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select fittingJob
            from FittingJob fittingJob
            join fetch fittingJob.member member
            where fittingJob.id = :fittingJobId
            """)
    Optional<FittingJob> findByIdWithMemberForResultSave(
            @Param("fittingJobId") Long fittingJobId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select fittingJob
            from FittingJob fittingJob
            join fetch fittingJob.member member
            where fittingJob.id = :fittingJobId
            """)
    Optional<FittingJob> findByIdWithMemberForUpdate(
            @Param("fittingJobId") Long fittingJobId
    );

    @Query("""
            select fittingJob
            from FittingJob fittingJob
            where fittingJob.member.id = :memberId
              and (
                    fittingJob.status = :pendingStatus
                    or (
                        fittingJob.status = :failedStatus
                        and fittingJob.resultViewedAt is null
                    )
                    or (
                        fittingJob.status = :completedStatus
                        and fittingJob.resultViewedAt is null
                        and exists (
                            select tempResult
                            from FittingTempResult tempResult
                            where tempResult.fittingJob = fittingJob
                              and tempResult.expiresAt > :now
                        )
                    )
              )
            order by
                case when fittingJob.status = :pendingStatus then 0 else 1 end,
                fittingJob.createdAt desc,
                fittingJob.id desc
            """)
    List<FittingJob> findCurrentJobs(
            @Param("memberId") Long memberId,
            @Param("pendingStatus") FittingJobStatus pendingStatus,
            @Param("completedStatus") FittingJobStatus completedStatus,
            @Param("failedStatus") FittingJobStatus failedStatus,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );
}
