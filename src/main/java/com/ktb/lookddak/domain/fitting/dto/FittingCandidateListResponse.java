package com.ktb.lookddak.domain.fitting.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;

import java.util.List;

@Getter
@JsonPropertyOrder({"items", "nextCursor", "hasNext"})
public class FittingCandidateListResponse {

    private final List<FittingCandidateListItemResponse> items;
    private final Long nextCursor;
    private final boolean hasNext;

    public FittingCandidateListResponse(
            List<FittingCandidateListItemResponse> items,
            Long nextCursor,
            boolean hasNext
    ) {
        this.items = List.copyOf(items);
        this.nextCursor = nextCursor;
        this.hasNext = hasNext;
    }
}
