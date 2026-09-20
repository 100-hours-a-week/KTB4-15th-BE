package com.ktb.lookddak.domain.chat.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class ChatRoomListResponse {

    private final List<ChatRoomListItemResponse> items;
    private final Long nextCursor;
    private final boolean hasNext;

    public ChatRoomListResponse(
            List<ChatRoomListItemResponse> items,
            Long nextCursor,
            boolean hasNext
    ) {
        this.items = List.copyOf(items);
        this.nextCursor = nextCursor;
        this.hasNext = hasNext;
    }
}
