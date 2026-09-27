package com.ktb.lookddak.domain.fitting.service;

import com.ktb.lookddak.domain.fitting.command.FittingGenerationCommand;
import com.ktb.lookddak.domain.fitting.entity.FittingJob;
import com.ktb.lookddak.domain.fitting.entity.FittingJobProduct;
import com.ktb.lookddak.domain.fitting.entity.FittingJobStatus;
import com.ktb.lookddak.domain.fitting.repository.FittingJobProductRepository;
import com.ktb.lookddak.domain.fitting.repository.FittingJobRepository;
import com.ktb.lookddak.domain.member.entity.MemberProfile;
import com.ktb.lookddak.domain.member.repository.MemberProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FittingGenerationQueryService {

    private final FittingJobRepository fittingJobRepository;
    private final FittingJobProductRepository fittingJobProductRepository;
    private final MemberProfileRepository memberProfileRepository;

    public FittingGenerationCommand createCommand(Long fittingJobId) {
        FittingJob fittingJob = fittingJobRepository
                .findByIdWithMember(fittingJobId)
                .orElseThrow(() -> new IllegalStateException(
                        "AI 요청을 생성할 가상피팅 작업이 없습니다."
                ));

        if (fittingJob.getStatus() != FittingJobStatus.GENERATING) {
            throw new IllegalStateException(
                    "생성 중인 가상피팅 작업만 AI에 요청할 수 있습니다."
            );
        }

        Long memberId = fittingJob.getMember().getId();
        MemberProfile memberProfile = memberProfileRepository
                .findByMemberId(memberId)
                .orElseThrow(() -> new IllegalStateException(
                        "AI 가상피팅에 사용할 회원 프로필이 없습니다."
                ));

        List<FittingJobProduct> jobProducts = fittingJobProductRepository
                .findAllByFittingJobIdWithProduct(fittingJobId);
        if (jobProducts.isEmpty()) {
            throw new IllegalStateException(
                    "AI 가상피팅에 사용할 상품이 없습니다."
            );
        }

        List<String> productCodes = new ArrayList<>();
        for (FittingJobProduct jobProduct : jobProducts) {
            productCodes.add(jobProduct.getProduct().getProductCode());
        }

        return new FittingGenerationCommand(
                fittingJobId,
                memberProfile.getFullBodyImageKey(),
                productCodes
        );
    }
}
