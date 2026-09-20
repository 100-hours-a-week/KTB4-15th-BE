package com.ktb.lookddak.domain.wishlist.dto;

import lombok.Getter;

@Getter
public class WishlistCreateResponse {

    private final Long wishlistId;
    private final Long productId;

    public WishlistCreateResponse(Long wishlistId, Long productId) {
        this.wishlistId = wishlistId;
        this.productId = productId;
    }
}
