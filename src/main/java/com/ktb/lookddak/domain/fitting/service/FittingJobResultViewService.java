package com.ktb.lookddak.domain.fitting.service;

import com.ktb.lookddak.domain.fitting.dto.FittingJobResultViewedResponse;
import com.ktb.lookddak.domain.fitting.entity.FittingJob;
import com.ktb.lookddak.domain.fitting.entity.FittingJobStatus;
import com.ktb.lookddak.domain.fitting.entity.FittingTempResult;
import com.ktb.lookddak.domain.fitting.repository.FittingJobRepository;
import com.ktb.lookddak.domain.fitting.repository.FittingTempResultRepository;
import com.ktb.lookddak.global.exception.BusinessException;
import com.ktb.lookddak.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FittingJobResultViewService {

    private final FittingJobRepository fittingJobRepository;
    private final FittingTempResultRepository fittingTempResultRepository;

    @Transactional
    public FittingJobResultViewedResponse markResultViewed(
            Long memberId,
            Long fittingJobId
    ) {
        FittingJob fittingJob = fittingJobRepository
                .findByIdWithMemberForUpdate(fittingJobId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.FITTING_JOB_NOT_FOUND
                ));

        if (!fittingJob.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.FITTING_JOB_ACCESS_DENIED);
        }

        if (fittingJob.getStatus() == FittingJobStatus.PENDING) {
            throw new BusinessException(ErrorCode.FITTING_JOB_NOT_COMPLETED);
        }

        if (fittingJob.getStatus() == FittingJobStatus.COMPLETED) {
            FittingTempResult tempResult = fittingTempResultRepository
                    .findByFittingJobId(fittingJobId)
                    .orElseThrow(() -> new BusinessException(
                            ErrorCode.FITTING_TEMP_RESULT_NOT_FOUND
                    ));

            if (tempResult.isExpired(LocalDateTime.now())) {
                throw new BusinessException(
                        ErrorCode.FITTING_TEMP_RESULT_NOT_FOUND
                );
            }
        }

        fittingJob.markResultViewed(LocalDateTime.now());
        return FittingJobResultViewedResponse.from(fittingJob);
    }
}
