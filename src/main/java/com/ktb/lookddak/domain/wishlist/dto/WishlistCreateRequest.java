package com.ktb.lookddak.domain.wishlist.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WishlistCreateRequest {

    @NotNull
    @Positive
    private Long productId;

    public WishlistCreateRequest(Long productId) {
        this.productId = productId;
    }
}
