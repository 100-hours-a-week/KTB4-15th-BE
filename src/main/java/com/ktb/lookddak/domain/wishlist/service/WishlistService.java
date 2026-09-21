package com.ktb.lookddak.domain.wishlist.service;

import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.member.repository.MemberRepository;
import com.ktb.lookddak.domain.product.entity.Product;
import com.ktb.lookddak.domain.product.repository.ProductRepository;
import com.ktb.lookddak.domain.wishlist.dto.WishlistCreateRequest;
import com.ktb.lookddak.domain.wishlist.dto.WishlistCreateResponse;
import com.ktb.lookddak.domain.wishlist.dto.WishlistCountResponse;
import com.ktb.lookddak.domain.wishlist.entity.Wishlist;
import com.ktb.lookddak.domain.wishlist.repository.WishlistRepository;
import com.ktb.lookddak.global.exception.BusinessException;
import com.ktb.lookddak.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WishlistService {

    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final WishlistRepository wishlistRepository;

    public WishlistCountResponse getWishlistCount(Long memberId) {
        long count = wishlistRepository.countByMemberId(memberId);

        return new WishlistCountResponse(count);
    }

    @Transactional
    public WishlistCreateResponse createWishlist(
            Long memberId,
            WishlistCreateRequest request
    ) {
        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.RESOURCE_NOT_FOUND)
                );
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.PRODUCT_NOT_FOUND)
                );

        if (wishlistRepository.existsByMemberIdAndProductId(
                memberId,
                product.getId()
        )) {
            throw new BusinessException(ErrorCode.WISHLIST_ALREADY_EXISTS);
        }

        Wishlist wishlist = Wishlist.create(member, product);

        try {
            Wishlist savedWishlist = wishlistRepository.saveAndFlush(wishlist);
            return new WishlistCreateResponse(
                    savedWishlist.getId(),
                    product.getId()
            );
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.WISHLIST_ALREADY_EXISTS);
        }
    }

    @Transactional
    public void deleteWishlist(Long memberId, Long wishlistId) {
        Wishlist wishlist = wishlistRepository.findByIdForUpdate(wishlistId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.WISHLIST_NOT_FOUND)
                );

        if (!wishlist.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.WISHLIST_ACCESS_DENIED);
        }

        wishlistRepository.delete(wishlist);
    }
}
