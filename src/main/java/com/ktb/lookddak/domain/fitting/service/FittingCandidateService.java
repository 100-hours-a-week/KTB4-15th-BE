package com.ktb.lookddak.domain.fitting.service;

import com.ktb.lookddak.domain.fitting.dto.FittingCandidateCreateRequest;
import com.ktb.lookddak.domain.fitting.dto.FittingCandidateCreateResponse;
import com.ktb.lookddak.domain.fitting.dto.FittingCandidateBulkDeleteRequest;
import com.ktb.lookddak.domain.fitting.dto.FittingCandidateBulkDeleteResponse;
import com.ktb.lookddak.domain.fitting.dto.FittingCandidateListItemResponse;
import com.ktb.lookddak.domain.fitting.dto.FittingCandidateListResponse;
import com.ktb.lookddak.domain.fitting.entity.FittingCandidate;
import com.ktb.lookddak.domain.fitting.repository.FittingCandidateRepository;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.member.repository.MemberRepository;
import com.ktb.lookddak.domain.product.entity.Product;
import com.ktb.lookddak.domain.product.entity.ProductItemType;
import com.ktb.lookddak.domain.product.repository.ProductRepository;
import com.ktb.lookddak.global.exception.BusinessException;
import com.ktb.lookddak.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FittingCandidateService {

    private static final long MAX_CANDIDATE_COUNT = 1_000L;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 100;

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

    @Transactional
    public FittingCandidateBulkDeleteResponse deleteFittingCandidates(
            Long memberId,
            FittingCandidateBulkDeleteRequest request
    ) {
        // 중복 ID는 한 번만 삭제 대상으로 처리한다.
        Set<Long> candidateIds = new LinkedHashSet<>(
                request.getFittingCandidateIds()
        );
        List<FittingCandidate> candidates = fittingCandidateRepository
                .findAllByIdInForUpdate(candidateIds);

        if (candidates.size() != candidateIds.size()) {
            throw new BusinessException(
                    ErrorCode.FITTING_CANDIDATE_NOT_FOUND
            );
        }

        for (FittingCandidate candidate : candidates) {
            if (!candidate.isOwnedBy(memberId)) {
                throw new BusinessException(
                        ErrorCode.FITTING_CANDIDATE_ACCESS_DENIED
                );
            }
        }

        fittingCandidateRepository.deleteAllInBatch(candidates);

        return new FittingCandidateBulkDeleteResponse(candidates.size());
    }

    public FittingCandidateListResponse getFittingCandidates(
            Long memberId,
            ProductItemType itemType,
            Long cursor,
            Integer size
    ) {
        int pageSize = size == null ? DEFAULT_PAGE_SIZE : size;
        validatePagination(cursor, pageSize);

        PageRequest pageRequest = PageRequest.of(0, pageSize + 1);
        List<FittingCandidate> candidates = findCandidates(
                memberId,
                itemType,
                cursor,
                pageRequest
        );

        boolean hasNext = candidates.size() > pageSize;
        List<FittingCandidate> responseCandidates = hasNext
                ? candidates.subList(0, pageSize)
                : candidates;

        List<FittingCandidateListItemResponse> items = new ArrayList<>();
        for (FittingCandidate candidate : responseCandidates) {
            items.add(FittingCandidateListItemResponse.from(candidate));
        }

        Long nextCursor = hasNext && !responseCandidates.isEmpty()
                ? responseCandidates.get(responseCandidates.size() - 1).getId()
                : null;

        long totalCount = itemType == null
                ? fittingCandidateRepository.countByMemberId(memberId)
                : fittingCandidateRepository.countByMemberIdAndProductItemType(
                        memberId,
                        itemType
                );

        return new FittingCandidateListResponse(
                totalCount,
                items,
                nextCursor,
                hasNext
        );
    }

    private List<FittingCandidate> findCandidates(
            Long memberId,
            ProductItemType itemType,
            Long cursor,
            PageRequest pageRequest
    ) {
        if (itemType == null && cursor == null) {
            return fittingCandidateRepository.findFirstPage(
                    memberId,
                    pageRequest
            );
        }
        if (itemType == null) {
            return fittingCandidateRepository.findNextPage(
                    memberId,
                    cursor,
                    pageRequest
            );
        }
        if (cursor == null) {
            return fittingCandidateRepository.findFirstPageByItemType(
                    memberId,
                    itemType,
                    pageRequest
            );
        }
        return fittingCandidateRepository.findNextPageByItemType(
                memberId,
                cursor,
                itemType,
                pageRequest
        );
    }

    private void validatePagination(Long cursor, int size) {
        if ((cursor != null && cursor <= 0)
                || size < MIN_PAGE_SIZE
                || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_PAGINATION_PARAMETER);
        }
    }
}
