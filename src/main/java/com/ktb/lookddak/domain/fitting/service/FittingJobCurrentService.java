package com.ktb.lookddak.domain.fitting.service;

import com.ktb.lookddak.domain.fitting.dto.FittingJobCurrentProductResponse;
import com.ktb.lookddak.domain.fitting.dto.FittingJobCurrentResponse;
import com.ktb.lookddak.domain.fitting.entity.FittingJob;
import com.ktb.lookddak.domain.fitting.entity.FittingJobProduct;
import com.ktb.lookddak.domain.fitting.entity.FittingJobStatus;
import com.ktb.lookddak.domain.fitting.repository.FittingJobProductRepository;
import com.ktb.lookddak.domain.fitting.repository.FittingJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FittingJobCurrentService {

    private final FittingJobRepository fittingJobRepository;
    private final FittingJobProductRepository fittingJobProductRepository;

    public FittingJobCurrentResponse getCurrentFittingJob(Long memberId) {
        List<FittingJob> fittingJobs = fittingJobRepository.findCurrentJobs(
                memberId,
                FittingJobStatus.GENERATING,
                FittingJobStatus.COMPLETED,
                FittingJobStatus.FAILED,
                LocalDateTime.now(),
                PageRequest.of(0, 1)
        );

        if (fittingJobs.isEmpty()) {
            return null;
        }

        FittingJob fittingJob = fittingJobs.getFirst();
        List<FittingJobProduct> jobProducts = fittingJobProductRepository
                .findAllByFittingJobIdWithProduct(fittingJob.getId());
        List<FittingJobCurrentProductResponse> products = new ArrayList<>();
        for (FittingJobProduct jobProduct : jobProducts) {
            products.add(FittingJobCurrentProductResponse.from(
                    jobProduct.getProduct()
            ));
        }

        return FittingJobCurrentResponse.from(fittingJob, products);
    }
}
