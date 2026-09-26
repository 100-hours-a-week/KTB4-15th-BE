package com.ktb.lookddak.domain.fitting.service;

import com.ktb.lookddak.domain.fitting.dto.FittingJobCreateRequest;
import com.ktb.lookddak.domain.fitting.dto.FittingJobCreateResponse;
import com.ktb.lookddak.domain.fitting.dto.FittingJobStatusResponse;
import com.ktb.lookddak.domain.fitting.dto.FittingResultProductResponse;
import com.ktb.lookddak.domain.fitting.dto.FittingResultResponse;
import com.ktb.lookddak.domain.fitting.entity.FittingJob;
import com.ktb.lookddak.domain.fitting.entity.FittingJobProduct;
import com.ktb.lookddak.domain.fitting.entity.FittingJobStatus;
import com.ktb.lookddak.domain.fitting.entity.FittingTempResult;
import com.ktb.lookddak.domain.fitting.event.FittingGenerationRequestedEvent;
import com.ktb.lookddak.domain.fitting.repository.FittingCandidateRepository;
import com.ktb.lookddak.domain.fitting.repository.FittingJobProductRepository;
import com.ktb.lookddak.domain.fitting.repository.FittingJobRepository;
import com.ktb.lookddak.domain.fitting.repository.FittingTempResultRepository;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.member.repository.MemberRepository;
import com.ktb.lookddak.domain.member.repository.MemberProfileRepository;
import com.ktb.lookddak.domain.product.entity.Product;
import com.ktb.lookddak.domain.product.entity.ProductItemType;
import com.ktb.lookddak.domain.product.repository.ProductRepository;
import com.ktb.lookddak.global.exception.BusinessException;
import com.ktb.lookddak.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FittingJobService {

    private final MemberRepository memberRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final ProductRepository productRepository;
    private final FittingCandidateRepository fittingCandidateRepository;
    private final FittingJobRepository fittingJobRepository;
    private final FittingJobProductRepository fittingJobProductRepository;
    private final FittingTempResultRepository fittingTempResultRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public FittingJobCreateResponse createFittingJob(
            Long memberId,
            FittingJobCreateRequest request
    ) {
        // 같은 회원의 동시 생성 요청을 직렬화해 GENERATING 작업 중복 생성을 막는다.
        Member member = memberRepository.findActiveByIdForUpdate(memberId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.RESOURCE_NOT_FOUND)
                );

        if (!memberProfileRepository.existsByMemberId(memberId)) {
            throw new BusinessException(
                    ErrorCode.MEMBER_PROFILE_NOT_FOUND
            );
        }

        if (fittingJobRepository.existsByMemberIdAndStatus(
                memberId,
                FittingJobStatus.GENERATING
        )) {
            throw new BusinessException(
                    ErrorCode.FITTING_JOB_ALREADY_GENERATING
            );
        }

        LinkedHashMap<Long, ProductItemType> requestedProducts =
                createRequestedProducts(request);
        Map<Long, Product> productsById = findProductsById(requestedProducts);

        validateProductTypes(requestedProducts, productsById);
        validateFittingCandidates(memberId, requestedProducts.keySet());

        FittingJob fittingJob = fittingJobRepository.save(
                FittingJob.create(member)
        );

        List<FittingJobProduct> jobProducts = new ArrayList<>();
        for (Long productId : requestedProducts.keySet()) {
            jobProducts.add(FittingJobProduct.create(
                    fittingJob,
                    productsById.get(productId)
            ));
        }
        fittingJobProductRepository.saveAll(jobProducts);
        eventPublisher.publishEvent(new FittingGenerationRequestedEvent(
                fittingJob.getId()
        ));

        return FittingJobCreateResponse.from(fittingJob);
    }

    public FittingJobStatusResponse getFittingJobStatus(
            Long memberId,
            Long fittingJobId
    ) {
        FittingJob fittingJob = fittingJobRepository
                .findByIdWithMember(fittingJobId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.FITTING_JOB_NOT_FOUND
                ));

        if (!fittingJob.isOwnedBy(memberId)) {
            throw new BusinessException(
                    ErrorCode.FITTING_JOB_ACCESS_DENIED
            );
        }

        // 생성 중이거나 실패한 작업에는 결과 데이터가 존재하지 않는다.
        if (fittingJob.getStatus() != FittingJobStatus.COMPLETED) {
            return FittingJobStatusResponse.from(fittingJob, null);
        }

        FittingTempResult tempResult = fittingTempResultRepository
                .findByFittingJobId(fittingJobId)
                .orElseThrow(() -> new IllegalStateException(
                        "완료된 가상피팅 작업의 결과가 존재하지 않습니다."
                ));
        List<FittingJobProduct> jobProducts = fittingJobProductRepository
                .findAllByFittingJobIdWithProduct(fittingJobId);

        if (jobProducts.isEmpty()) {
            throw new IllegalStateException(
                    "완료된 가상피팅 작업의 상품이 존재하지 않습니다."
            );
        }

        List<FittingResultProductResponse> productResponses =
                new ArrayList<>();
        for (FittingJobProduct jobProduct : jobProducts) {
            productResponses.add(FittingResultProductResponse.from(
                    jobProduct.getProduct()
            ));
        }

        FittingResultResponse resultResponse = FittingResultResponse.from(
                tempResult,
                productResponses
        );
        return FittingJobStatusResponse.from(fittingJob, resultResponse);
    }

    private LinkedHashMap<Long, ProductItemType> createRequestedProducts(
            FittingJobCreateRequest request
    ) {
        LinkedHashMap<Long, ProductItemType> requestedProducts =
                new LinkedHashMap<>();

        if (request.getTopProductId() != null) {
            requestedProducts.put(
                    request.getTopProductId(),
                    ProductItemType.TOP
            );
        }
        if (request.getBottomProductId() != null) {
            requestedProducts.put(
                    request.getBottomProductId(),
                    ProductItemType.BOTTOM
            );
        }

        if (requestedProducts.isEmpty()) {
            throw new BusinessException(ErrorCode.FITTING_PRODUCT_REQUIRED);
        }

        return requestedProducts;
    }

    private Map<Long, Product> findProductsById(
            LinkedHashMap<Long, ProductItemType> requestedProducts
    ) {
        List<Product> products = productRepository.findAllById(
                requestedProducts.keySet()
        );
        Map<Long, Product> productsById = new HashMap<>();
        for (Product product : products) {
            productsById.put(product.getId(), product);
        }

        if (productsById.size() != requestedProducts.size()) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        return productsById;
    }

    private void validateProductTypes(
            LinkedHashMap<Long, ProductItemType> requestedProducts,
            Map<Long, Product> productsById
    ) {
        for (Map.Entry<Long, ProductItemType> entry
                : requestedProducts.entrySet()) {
            Product product = productsById.get(entry.getKey());
            if (product.getItemType() != entry.getValue()) {
                throw new BusinessException(
                        ErrorCode.FITTING_PRODUCT_TYPE_MISMATCH
                );
            }
        }
    }

    private void validateFittingCandidates(
            Long memberId,
            Set<Long> requestedProductIds
    ) {
        Set<Long> candidateProductIds = fittingCandidateRepository
                .findProductIdsByMemberIdAndProductIdIn(
                        memberId,
                        requestedProductIds
                );

        if (!candidateProductIds.containsAll(requestedProductIds)) {
            throw new BusinessException(
                    ErrorCode.FITTING_PRODUCT_NOT_CANDIDATE
            );
        }
    }
}
