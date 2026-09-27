package com.ktb.lookddak.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;

import java.util.List;

@Getter
@JsonPropertyOrder({"totalCount", "items", "nextCursor", "hasNext"})
public class ChatRoomListResponse {

    private final long totalCount;
    private final List<ChatRoomListItemResponse> items;
    private final Long nextCursor;
    private final boolean hasNext;

    public ChatRoomListResponse(
            long totalCount,
            List<ChatRoomListItemResponse> items,
            Long nextCursor,
            boolean hasNext
    ) {
        this.totalCount = totalCount;
        this.items = List.copyOf(items);
        this.nextCursor = nextCursor;
        this.hasNext = hasNext;
    }
}
