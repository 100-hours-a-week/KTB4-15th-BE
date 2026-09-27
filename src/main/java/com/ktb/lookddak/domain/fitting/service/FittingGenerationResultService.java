package com.ktb.lookddak.domain.fitting.service;

import com.ktb.lookddak.domain.fitting.entity.FittingJob;
import com.ktb.lookddak.domain.fitting.entity.FittingJobStatus;
import com.ktb.lookddak.domain.fitting.entity.FittingTempResult;
import com.ktb.lookddak.domain.fitting.repository.FittingJobRepository;
import com.ktb.lookddak.domain.fitting.repository.FittingTempResultRepository;
import com.ktb.lookddak.global.client.ai.fitting.dto.AiFittingResultData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FittingGenerationResultService {

    private final FittingJobRepository fittingJobRepository;
    private final FittingTempResultRepository fittingTempResultRepository;

    @Transactional
    public void complete(
            Long fittingJobId,
            AiFittingResultData result
    ) {
        FittingJob fittingJob = getFittingJobForUpdate(fittingJobId);
        if (!isGenerating(fittingJob)) {
            return;
        }

        fittingTempResultRepository.save(FittingTempResult.create(
                fittingJob,
                result.getResultImageKey(),
                result.getLlmTitle(),
                result.getLlmComment()
        ));
        fittingJob.completeGeneration();
    }

    @Transactional
    public void fail(Long fittingJobId) {
        FittingJob fittingJob = getFittingJobForUpdate(fittingJobId);
        if (!isGenerating(fittingJob)) {
            return;
        }

        fittingJob.failGeneration();
    }

    private FittingJob getFittingJobForUpdate(Long fittingJobId) {
        return fittingJobRepository
                .findByIdForGenerationUpdate(fittingJobId)
                .orElseThrow(() -> new IllegalStateException(
                        "AI 결과를 연결할 가상피팅 작업이 없습니다."
                ));
    }

    private boolean isGenerating(FittingJob fittingJob) {
        return fittingJob.getStatus() == FittingJobStatus.GENERATING;
    }
}
