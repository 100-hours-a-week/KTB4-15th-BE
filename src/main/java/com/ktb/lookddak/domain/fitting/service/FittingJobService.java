package com.ktb.lookddak.domain.fitting.service;

import com.ktb.lookddak.domain.fitting.dto.FittingJobCreateRequest;
import com.ktb.lookddak.domain.fitting.dto.FittingJobCreateResponse;
import com.ktb.lookddak.domain.fitting.entity.FittingJob;
import com.ktb.lookddak.domain.fitting.entity.FittingJobProduct;
import com.ktb.lookddak.domain.fitting.entity.FittingJobStatus;
import com.ktb.lookddak.domain.fitting.repository.FittingCandidateRepository;
import com.ktb.lookddak.domain.fitting.repository.FittingJobProductRepository;
import com.ktb.lookddak.domain.fitting.repository.FittingJobRepository;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.member.repository.MemberRepository;
import com.ktb.lookddak.domain.product.entity.Product;
import com.ktb.lookddak.domain.product.entity.ProductItemType;
import com.ktb.lookddak.domain.product.repository.ProductRepository;
import com.ktb.lookddak.global.exception.BusinessException;
import com.ktb.lookddak.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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
    private final ProductRepository productRepository;
    private final FittingCandidateRepository fittingCandidateRepository;
    private final FittingJobRepository fittingJobRepository;
    private final FittingJobProductRepository fittingJobProductRepository;

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

        return FittingJobCreateResponse.from(fittingJob);
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
