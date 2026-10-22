package com.ktb.lookddak.domain.wishlist.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;

import java.util.List;

@Getter
@JsonPropertyOrder({"totalCount", "items", "nextCursor", "hasNext"})
public class WishlistListResponse {

    private final long totalCount;
    private final List<WishlistListItemResponse> items;
    private final Long nextCursor;
    private final boolean hasNext;

    public WishlistListResponse(
            long totalCount,
            List<WishlistListItemResponse> items,
            Long nextCursor,
            boolean hasNext
    ) {
        this.totalCount = totalCount;
        this.items = items;
        this.nextCursor = nextCursor;
        this.hasNext = hasNext;
    }
}
