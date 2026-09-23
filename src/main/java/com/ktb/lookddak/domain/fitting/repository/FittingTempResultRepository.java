package com.ktb.lookddak.domain.fitting.repository;

import com.ktb.lookddak.domain.fitting.entity.FittingTempResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FittingTempResultRepository
        extends JpaRepository<FittingTempResult, Long> {

    Optional<FittingTempResult> findByFittingJobId(Long fittingJobId);
}
