package com.ktb.lookddak.domain.wishlist.dto;

import com.ktb.lookddak.domain.wishlist.entity.Wishlist;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
public class WishlistListItemResponse {

    private final Long wishlistId;
    private final Long productId;
    private final String productName;
    private final String productImageUrl;
    private final String itemType;
    private final Integer wishedPrice;
    private final Integer currentPrice;
    private final Integer priceChangeRate;
    private final String purchaseUrl;
    private final java.time.LocalDateTime wishedAt;

    private WishlistListItemResponse(Wishlist wishlist, Integer priceChangeRate) {
        this.wishlistId = wishlist.getId();
        this.productId = wishlist.getProduct().getId();
        this.productName = wishlist.getProduct().getName();
        this.productImageUrl = wishlist.getProduct().getImageUrl();
        this.itemType = wishlist.getProduct().getItemType().name();
        this.wishedPrice = wishlist.getWishedPrice();
        this.currentPrice = wishlist.getProduct().getCurrentPrice();
        this.priceChangeRate = priceChangeRate;
        this.purchaseUrl = wishlist.getProduct().getPurchaseUrl();
        this.wishedAt = wishlist.getCreatedAt();
    }

    public static WishlistListItemResponse from(Wishlist wishlist) {
        int wishedPrice = wishlist.getWishedPrice();
        int currentPrice = wishlist.getProduct().getCurrentPrice();
        int priceChangeRate = BigDecimal.valueOf(currentPrice - wishedPrice)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(wishedPrice), 0, RoundingMode.HALF_UP)
                .intValue();

        return new WishlistListItemResponse(wishlist, priceChangeRate);
    }
}
