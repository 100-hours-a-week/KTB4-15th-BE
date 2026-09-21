package com.ktb.lookddak.domain.wishlist.dto;

import lombok.Getter;

@Getter
public class WishlistCountResponse {

    private final long count;

    public WishlistCountResponse(long count) {
        this.count = count;
    }
}
