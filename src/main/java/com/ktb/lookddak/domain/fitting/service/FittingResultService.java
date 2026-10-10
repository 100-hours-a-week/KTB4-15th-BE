package com.ktb.lookddak.domain.fitting.service;

import com.ktb.lookddak.domain.fitting.dto.FittingResultCreateRequest;
import com.ktb.lookddak.domain.fitting.dto.FittingResultCreateResponse;
import com.ktb.lookddak.domain.fitting.dto.FittingResultDetailResponse;
import com.ktb.lookddak.domain.fitting.dto.FittingResultListItemResponse;
import com.ktb.lookddak.domain.fitting.dto.FittingResultListResponse;
import com.ktb.lookddak.domain.fitting.dto.FittingResultOutfitNameUpdateRequest;
import com.ktb.lookddak.domain.fitting.dto.FittingResultOutfitNameUpdateResponse;
import com.ktb.lookddak.domain.fitting.dto.FittingResultProductResponse;
import com.ktb.lookddak.domain.fitting.entity.FittingJob;
import com.ktb.lookddak.domain.fitting.entity.FittingJobProduct;
import com.ktb.lookddak.domain.fitting.entity.FittingJobStatus;
import com.ktb.lookddak.domain.fitting.entity.FittingResult;
import com.ktb.lookddak.domain.fitting.entity.FittingTempResult;
import com.ktb.lookddak.domain.fitting.repository.FittingJobRepository;
import com.ktb.lookddak.domain.fitting.repository.FittingJobProductRepository;
import com.ktb.lookddak.domain.fitting.repository.FittingResultRepository;
import com.ktb.lookddak.domain.fitting.repository.FittingTempResultRepository;
import com.ktb.lookddak.global.exception.BusinessException;
import com.ktb.lookddak.global.exception.ErrorCode;
import com.ktb.lookddak.global.storage.s3.S3PresignedUrlProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FittingResultService {

    private static final int MAX_OUTFIT_NAME_LENGTH = 20;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 100;

    private final FittingJobRepository fittingJobRepository;
    private final FittingJobProductRepository fittingJobProductRepository;
    private final FittingTempResultRepository fittingTempResultRepository;
    private final FittingResultRepository fittingResultRepository;
    private final S3PresignedUrlProvider presignedUrlProvider;

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
            FittingResult savedResult = fittingResultRepository.saveAndFlush(
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

    /**
     * 로그인한 회원이 보관한 가상피팅 결과를 최신 저장 순으로 조회한다.
     * 한 건을 더 조회해 다음 페이지 존재 여부를 판단한다.
     */
    public FittingResultListResponse getFittingResults(
            Long memberId,
            Long cursor,
            Integer size
    ) {
        int pageSize = size == null ? DEFAULT_PAGE_SIZE : size;
        validatePagination(cursor, pageSize);

        PageRequest pageRequest = PageRequest.of(0, pageSize + 1);
        List<FittingResult> results = cursor == null
                ? fittingResultRepository.findFirstPage(memberId, pageRequest)
                : fittingResultRepository.findNextPage(
                        memberId,
                        cursor,
                        pageRequest
                );

        boolean hasNext = results.size() > pageSize;
        List<FittingResult> responseResults = hasNext
                ? results.subList(0, pageSize)
                : results;

        List<FittingResultListItemResponse> items = new ArrayList<>();
        for (FittingResult result : responseResults) {
            String resultImageUrl = presignedUrlProvider.createGetUrl(
                    result.getResultImageKey()
            );
            items.add(FittingResultListItemResponse.from(result, resultImageUrl));
        }

        Long nextCursor = hasNext && !responseResults.isEmpty()
                ? responseResults.get(responseResults.size() - 1).getId()
                : null;

        long totalCount = fittingResultRepository.countActiveByMemberId(memberId);

        return new FittingResultListResponse(
                items,
                totalCount,
                nextCursor,
                hasNext
        );
    }

    /**
     * 저장된 가상피팅 결과와 해당 작업에 사용한 현재 상품 정보를 조회한다.
     */
    public FittingResultDetailResponse getFittingResult(
            Long memberId,
            Long fittingResultId
    ) {
        FittingResult fittingResult = fittingResultRepository
                .findActiveByIdWithMember(fittingResultId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.FITTING_RESULT_NOT_FOUND
                ));

        if (!fittingResult.isOwnedBy(memberId)) {
            throw new BusinessException(
                    ErrorCode.FITTING_RESULT_ACCESS_DENIED
            );
        }

        List<FittingJobProduct> jobProducts = fittingJobProductRepository
                .findAllByFittingJobIdWithProduct(
                        fittingResult.getFittingJob().getId()
                );
        List<FittingResultProductResponse> products = new ArrayList<>();

        for (FittingJobProduct jobProduct : jobProducts) {
            products.add(FittingResultProductResponse.from(
                    jobProduct.getProduct()
            ));
        }

        String resultImageUrl = presignedUrlProvider.createGetUrl(
                fittingResult.getResultImageKey()
        );

        return FittingResultDetailResponse.from(
                fittingResult,
                resultImageUrl,
                products
        );
    }

    /**
     * 사용자가 저장한 가상피팅 결과를 soft delete 처리한다.
     * 수정 대상 행을 잠가 동시에 들어온 삭제 요청을 안전하게 직렬화한다.
     */
    @Transactional
    public void deleteFittingResult(Long memberId, Long fittingResultId) {
        FittingResult fittingResult = fittingResultRepository
                .findActiveByIdWithMemberForUpdate(fittingResultId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.FITTING_RESULT_NOT_FOUND
                ));

        if (!fittingResult.isOwnedBy(memberId)) {
            throw new BusinessException(
                    ErrorCode.FITTING_RESULT_ACCESS_DENIED
            );
        }

        fittingResult.delete();
    }

    /**
     * 사용자가 저장한 가상피팅 결과의 코디명만 변경한다.
     */
    @Transactional
    public FittingResultOutfitNameUpdateResponse updateOutfitName(
            Long memberId,
            Long fittingResultId,
            FittingResultOutfitNameUpdateRequest request
    ) {
        validateOutfitName(request.getOutfitName());

        FittingResult fittingResult = fittingResultRepository
                .findActiveByIdWithMemberForUpdate(fittingResultId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.FITTING_RESULT_NOT_FOUND
                ));

        if (!fittingResult.isOwnedBy(memberId)) {
            throw new BusinessException(
                    ErrorCode.FITTING_RESULT_ACCESS_DENIED
            );
        }

        fittingResult.updateOutfitName(request.getOutfitName());

        return FittingResultOutfitNameUpdateResponse.from(fittingResult);
    }

    private void validateOutfitName(String outfitName) {
        if (outfitName == null || outfitName.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_OUTFIT_NAME);
        }

        if (outfitName.length() > MAX_OUTFIT_NAME_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_OUTFIT_NAME_LENGTH);
        }
    }

    private void validatePagination(Long cursor, int size) {
        if ((cursor != null && cursor <= 0)
                || size < MIN_PAGE_SIZE
                || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_PAGINATION_PARAMETER);
        }
    }
}
