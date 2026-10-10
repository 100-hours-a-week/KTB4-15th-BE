package com.ktb.lookddak.domain.fitting.service;

import com.ktb.lookddak.domain.fitting.dto.FittingResultCreateRequest;
import com.ktb.lookddak.domain.fitting.dto.FittingResultCreateResponse;
import com.ktb.lookddak.domain.fitting.entity.FittingJob;
import com.ktb.lookddak.domain.fitting.entity.FittingJobStatus;
import com.ktb.lookddak.domain.fitting.entity.FittingResult;
import com.ktb.lookddak.domain.fitting.entity.FittingTempResult;
import com.ktb.lookddak.domain.fitting.repository.FittingJobRepository;
import com.ktb.lookddak.domain.fitting.repository.FittingResultRepository;
import com.ktb.lookddak.domain.fitting.repository.FittingTempResultRepository;
import com.ktb.lookddak.global.exception.BusinessException;
import com.ktb.lookddak.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FittingResultService {

    private static final int MAX_OUTFIT_NAME_LENGTH = 20;

    private final FittingJobRepository fittingJobRepository;
    private final FittingTempResultRepository fittingTempResultRepository;
    private final FittingResultRepository fittingResultRepository;

    /**
     * 완료된 가상피팅의 임시 결과를 사용자가 보관할 최종 결과로 복사한다.
     * FittingJob 행을 잠가 같은 작업의 동시 저장 요청을 직렬화한다.
     */
    @Transactional
    public FittingResultCreateResponse createFittingResult(
            Long memberId,
            FittingResultCreateRequest request
    ) {
        validateOutfitName(request.getOutfitName());

        FittingJob fittingJob = fittingJobRepository
                .findByIdWithMemberForResultSave(request.getFittingJobId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.FITTING_JOB_NOT_FOUND
                ));

        if (!fittingJob.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.FITTING_JOB_ACCESS_DENIED);
        }

        if (fittingJob.getStatus() != FittingJobStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.FITTING_JOB_NOT_COMPLETED);
        }

        FittingTempResult tempResult = fittingTempResultRepository
                .findByFittingJobId(fittingJob.getId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.FITTING_TEMP_RESULT_NOT_FOUND
                ));

        if (fittingResultRepository.existsByFittingJobId(fittingJob.getId())) {
            throw new BusinessException(ErrorCode.FITTING_RESULT_ALREADY_SAVED);
        }

        try {
            FittingResult savedResult = fittingResultRepository.save(
                    FittingResult.create(
                            fittingJob,
                            tempResult.getResultImageKey(),
                            request.getOutfitName(),
                            tempResult.getAiComment()
                    )
            );
            return FittingResultCreateResponse.from(savedResult);
        } catch (DataIntegrityViolationException exception) {
            // DB UNIQUE(fitting_job_id) 제약이 동시 요청의 마지막 안전장치다.
            throw new BusinessException(ErrorCode.FITTING_RESULT_ALREADY_SAVED);
        }
    }

    private void validateOutfitName(String outfitName) {
        if (outfitName == null || outfitName.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_OUTFIT_NAME);
        }

        if (outfitName.length() > MAX_OUTFIT_NAME_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_OUTFIT_NAME_LENGTH);
        }
    }
}
