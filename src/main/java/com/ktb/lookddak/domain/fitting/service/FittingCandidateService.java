package com.ktb.lookddak.domain.fitting.service;

import com.ktb.lookddak.domain.fitting.dto.FittingCandidateCreateRequest;
import com.ktb.lookddak.domain.fitting.dto.FittingCandidateCreateResponse;
import com.ktb.lookddak.domain.fitting.entity.FittingCandidate;
import com.ktb.lookddak.domain.fitting.repository.FittingCandidateRepository;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.member.repository.MemberRepository;
import com.ktb.lookddak.domain.product.entity.Product;
import com.ktb.lookddak.domain.product.repository.ProductRepository;
import com.ktb.lookddak.global.exception.BusinessException;
import com.ktb.lookddak.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FittingCandidateService {

    private static final long MAX_CANDIDATE_COUNT = 1_000L;

    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final FittingCandidateRepository fittingCandidateRepository;

    @Transactional
    public FittingCandidateCreateResponse createFittingCandidate(
            Long memberId,
            FittingCandidateCreateRequest request
    ) {
        Member member = memberRepository.findActiveByIdForUpdate(memberId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.RESOURCE_NOT_FOUND)
                );
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.PRODUCT_NOT_FOUND)
                );

        if (fittingCandidateRepository.existsByMemberIdAndProductId(
                memberId,
                product.getId()
        )) {
            throw new BusinessException(
                    ErrorCode.FITTING_CANDIDATE_ALREADY_EXISTS
            );
        }

        long candidateCount = fittingCandidateRepository.countByMemberId(memberId);
        if (candidateCount >= MAX_CANDIDATE_COUNT) {
            throw new BusinessException(
                    ErrorCode.FITTING_CANDIDATE_LIMIT_EXCEEDED
            );
        }

        FittingCandidate candidate = FittingCandidate.create(member, product);

        try {
            FittingCandidate savedCandidate =
                    fittingCandidateRepository.saveAndFlush(candidate);
            return new FittingCandidateCreateResponse(
                    savedCandidate.getId(),
                    product.getId()
            );
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(
                    ErrorCode.FITTING_CANDIDATE_ALREADY_EXISTS
            );
        }
    }

    @Transactional
    public void deleteFittingCandidate(Long memberId, Long candidateId) {
        FittingCandidate candidate = fittingCandidateRepository
                .findByIdForUpdate(candidateId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.FITTING_CANDIDATE_NOT_FOUND
                ));

        if (!candidate.isOwnedBy(memberId)) {
            throw new BusinessException(
                    ErrorCode.FITTING_CANDIDATE_ACCESS_DENIED
            );
        }

        fittingCandidateRepository.delete(candidate);
    }
}
