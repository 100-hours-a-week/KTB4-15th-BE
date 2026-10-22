package com.ktb.lookddak.domain.chat.service;

import com.ktb.lookddak.domain.chat.dto.RecommendedProductResponse;
import com.ktb.lookddak.domain.fitting.entity.FittingCandidate;
import com.ktb.lookddak.domain.fitting.repository.FittingCandidateRepository;
import com.ktb.lookddak.domain.recommendation.entity.RecommendationProduct;
import com.ktb.lookddak.domain.wishlist.entity.Wishlist;
import com.ktb.lookddak.domain.wishlist.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RecommendedProductResponseAssembler {

    private final WishlistRepository wishlistRepository;
    private final FittingCandidateRepository fittingCandidateRepository;

    /**
     * 추천 상품과 현재 회원의 찜·피팅 후보 상태를 한 번씩 조회해 응답으로 조립한다.
     * 채팅방 상세 조회와 SSE done이 같은 응답 규칙을 사용한다.
     */
    public Map<RecommendationProduct, RecommendedProductResponse> assemble(
            Long memberId,
            List<RecommendationProduct> recommendationProducts
    ) {
        if (recommendationProducts.isEmpty()) {
            return Map.of();
        }

        Set<Long> productIds = new LinkedHashSet<>();
        for (RecommendationProduct recommendationProduct : recommendationProducts) {
            productIds.add(recommendationProduct.getProduct().getId());
        }

        Map<Long, Long> wishlistIdByProductId = findWishlistIds(
                memberId,
                productIds
        );
        Map<Long, Long> fittingCandidateIdByProductId =
                findFittingCandidateIds(memberId, productIds);

        Map<RecommendationProduct, RecommendedProductResponse> responses =
                new LinkedHashMap<>();
        for (RecommendationProduct recommendationProduct : recommendationProducts) {
            Long productId = recommendationProduct.getProduct().getId();
            responses.put(
                    recommendationProduct,
                    RecommendedProductResponse.from(
                            recommendationProduct,
                            wishlistIdByProductId.get(productId),
                            fittingCandidateIdByProductId.get(productId)
                    )
            );
        }
        return responses;
    }

    private Map<Long, Long> findWishlistIds(
            Long memberId,
            Set<Long> productIds
    ) {
        List<Wishlist> wishlists = wishlistRepository
                .findAllByMemberIdAndProductIdIn(memberId, productIds);
        Map<Long, Long> wishlistIdByProductId = new LinkedHashMap<>();
        for (Wishlist wishlist : wishlists) {
            wishlistIdByProductId.put(
                    wishlist.getProduct().getId(),
                    wishlist.getId()
            );
        }
        return wishlistIdByProductId;
    }

    private Map<Long, Long> findFittingCandidateIds(
            Long memberId,
            Set<Long> productIds
    ) {
        List<FittingCandidate> fittingCandidates = fittingCandidateRepository
                .findAllByMemberIdAndProductIdIn(memberId, productIds);
        Map<Long, Long> fittingCandidateIdByProductId = new LinkedHashMap<>();
        for (FittingCandidate fittingCandidate : fittingCandidates) {
            fittingCandidateIdByProductId.put(
                    fittingCandidate.getProduct().getId(),
                    fittingCandidate.getId()
            );
        }
        return fittingCandidateIdByProductId;
    }
}
