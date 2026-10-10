package com.ktb.lookddak.domain.fitting.repository;

import com.ktb.lookddak.domain.fitting.entity.FittingResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FittingResultRepository extends JpaRepository<FittingResult, Long> {

    boolean existsByFittingJobId(Long fittingJobId);
}
