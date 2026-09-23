package com.ktb.lookddak.domain.fitting.repository;

import com.ktb.lookddak.domain.fitting.entity.FittingJob;
import com.ktb.lookddak.domain.fitting.entity.FittingJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FittingJobRepository extends JpaRepository<FittingJob, Long> {

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
