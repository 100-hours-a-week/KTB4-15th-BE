package com.ktb.lookddak.domain.fitting.repository;

import com.ktb.lookddak.domain.fitting.entity.FittingResult;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FittingResultRepository extends JpaRepository<FittingResult, Long> {

    boolean existsByFittingJobId(Long fittingJobId);

    @Query("""
            select fittingResult
            from FittingResult fittingResult
            join fetch fittingResult.fittingJob fittingJob
            join fetch fittingJob.member member
            where fittingResult.id = :fittingResultId
              and fittingResult.deletedAt is null
            """)
    Optional<FittingResult> findActiveByIdWithMember(
            @Param("fittingResultId") Long fittingResultId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select fittingResult
            from FittingResult fittingResult
            join fetch fittingResult.fittingJob fittingJob
            join fetch fittingJob.member member
            where fittingResult.id = :fittingResultId
              and fittingResult.deletedAt is null
            """)
    Optional<FittingResult> findActiveByIdWithMemberForUpdate(
            @Param("fittingResultId") Long fittingResultId
    );

    @Query("""
            select fittingResult
            from FittingResult fittingResult
            where fittingResult.fittingJob.member.id = :memberId
              and fittingResult.deletedAt is null
            order by fittingResult.id desc
            """)
    List<FittingResult> findFirstPage(
            @Param("memberId") Long memberId,
            Pageable pageable
    );

    @Query("""
            select fittingResult
            from FittingResult fittingResult
            where fittingResult.fittingJob.member.id = :memberId
              and fittingResult.deletedAt is null
              and fittingResult.id < :cursor
            order by fittingResult.id desc
            """)
    List<FittingResult> findNextPage(
            @Param("memberId") Long memberId,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    @Query("""
            select count(fittingResult)
            from FittingResult fittingResult
            where fittingResult.fittingJob.member.id = :memberId
              and fittingResult.deletedAt is null
            """)
    long countActiveByMemberId(@Param("memberId") Long memberId);
}
