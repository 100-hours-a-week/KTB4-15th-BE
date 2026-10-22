package com.ktb.lookddak.domain.fitting.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;

import java.util.List;

@Getter
@JsonPropertyOrder({"items", "totalCount", "nextCursor", "hasNext"})
public class FittingResultListResponse {

    private final List<FittingResultListItemResponse> items;
    private final long totalCount;
    private final Long nextCursor;
    private final boolean hasNext;

    public FittingResultListResponse(
            List<FittingResultListItemResponse> items,
            long totalCount,
            Long nextCursor,
            boolean hasNext
    ) {
        this.items = items;
        this.totalCount = totalCount;
        this.nextCursor = nextCursor;
        this.hasNext = hasNext;
    }
}
