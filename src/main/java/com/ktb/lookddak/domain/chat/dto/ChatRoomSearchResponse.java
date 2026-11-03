package com.ktb.lookddak.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;

import java.util.List;

@Getter
@JsonPropertyOrder({"items", "nextCursor", "hasNext"})
public class ChatRoomSearchResponse {

    private final List<ChatRoomSearchItemResponse> items;
    private final Long nextCursor;
    private final boolean hasNext;

    public ChatRoomSearchResponse(
            List<ChatRoomSearchItemResponse> items,
            Long nextCursor,
            boolean hasNext
    ) {
        this.items = List.copyOf(items);
        this.nextCursor = nextCursor;
        this.hasNext = hasNext;
    }
}
