package com.ktb.lookddak.domain.fitting.repository;

import com.ktb.lookddak.domain.fitting.entity.FittingJob;
import com.ktb.lookddak.domain.fitting.entity.FittingJobStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

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
}
