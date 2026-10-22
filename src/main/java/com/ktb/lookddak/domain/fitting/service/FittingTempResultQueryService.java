package com.ktb.lookddak.domain.fitting.service;

import com.ktb.lookddak.domain.fitting.dto.FittingResultProductResponse;
import com.ktb.lookddak.domain.fitting.dto.FittingTempResultDetailResponse;
import com.ktb.lookddak.domain.fitting.entity.FittingJob;
import com.ktb.lookddak.domain.fitting.entity.FittingJobProduct;
import com.ktb.lookddak.domain.fitting.entity.FittingJobStatus;
import com.ktb.lookddak.domain.fitting.entity.FittingResult;
import com.ktb.lookddak.domain.fitting.entity.FittingTempResult;
import com.ktb.lookddak.domain.fitting.repository.FittingJobProductRepository;
import com.ktb.lookddak.domain.fitting.repository.FittingJobRepository;
import com.ktb.lookddak.domain.fitting.repository.FittingResultRepository;
import com.ktb.lookddak.domain.fitting.repository.FittingTempResultRepository;
import com.ktb.lookddak.global.exception.BusinessException;
import com.ktb.lookddak.global.exception.ErrorCode;
import com.ktb.lookddak.global.storage.s3.S3PresignedUrlProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FittingTempResultQueryService {

    private final FittingJobRepository fittingJobRepository;
    private final FittingTempResultRepository fittingTempResultRepository;
    private final FittingJobProductRepository fittingJobProductRepository;
    private final FittingResultRepository fittingResultRepository;
    private final S3PresignedUrlProvider presignedUrlProvider;

    public FittingTempResultDetailResponse getTempResult(
            Long memberId,
            Long fittingJobId
    ) {
        FittingJob fittingJob = fittingJobRepository
                .findByIdWithMember(fittingJobId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.FITTING_JOB_NOT_FOUND
                ));

        if (!fittingJob.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.FITTING_JOB_ACCESS_DENIED);
        }

        validateCompleted(fittingJob);

        FittingTempResult tempResult = fittingTempResultRepository
                .findByFittingJobId(fittingJobId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.FITTING_TEMP_RESULT_NOT_FOUND
                ));
        if (tempResult.isExpired(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.FITTING_TEMP_RESULT_NOT_FOUND);
        }

        List<FittingJobProduct> jobProducts = fittingJobProductRepository
                .findAllByFittingJobIdWithProduct(fittingJobId);
        List<FittingResultProductResponse> products = new ArrayList<>();
        for (FittingJobProduct jobProduct : jobProducts) {
            products.add(FittingResultProductResponse.from(
                    jobProduct.getProduct()
            ));
        }

        FittingResult fittingResult = fittingResultRepository
                .findByFittingJobId(fittingJobId)
                .orElse(null);

        return FittingTempResultDetailResponse.from(
                fittingJob,
                tempResult,
                fittingResult,
                presignedUrlProvider.createGetUrl(
                        tempResult.getResultImageKey()
                ),
                products
        );
    }

    private void validateCompleted(FittingJob fittingJob) {
        if (fittingJob.getStatus() == FittingJobStatus.GENERATING) {
            throw new BusinessException(ErrorCode.FITTING_JOB_NOT_COMPLETED);
        }
        if (fittingJob.getStatus() == FittingJobStatus.FAILED) {
            throw new BusinessException(ErrorCode.FITTING_JOB_FAILED);
        }
    }
}
