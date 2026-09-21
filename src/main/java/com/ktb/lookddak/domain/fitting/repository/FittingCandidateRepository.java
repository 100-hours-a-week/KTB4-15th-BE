package com.ktb.lookddak.domain.fitting.repository;

import com.ktb.lookddak.domain.fitting.entity.FittingCandidate;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FittingCandidateRepository
        extends JpaRepository<FittingCandidate, Long> {

    boolean existsByMemberIdAndProductId(Long memberId, Long productId);

    long countByMemberId(Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select fc from FittingCandidate fc where fc.id = :candidateId")
    Optional<FittingCandidate> findByIdForUpdate(
            @Param("candidateId") Long candidateId
    );
}
