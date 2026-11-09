package com.ktb.lookddak.domain.chat.redis;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.ktb.lookddak.domain.chat.dto.RecommendedProductResponse;

import java.util.List;

/**
 * Redis Pub/Sub과 로컬 SSE 전달 사이의 공통 이벤트 계약이다.
 * seq는 userMessageId 단위로 증가하며 Snapshot과 이벤트의 순서를 맞추는 기준이다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "userMessageId", "seq", "type", "content", "products", "code", "message"
})
public record ChatRedisEvent(
        Long userMessageId,
        long seq,
        ChatRedisEventType type,
        String content,
        List<RecommendedProductResponse> products,
        String code,
        String message
) {

    public ChatRedisEvent {
        products = products == null ? null : List.copyOf(products);
    }

    public static ChatRedisEvent token(
            Long userMessageId,
            long seq,
            String content
    ) {
        return new ChatRedisEvent(
                userMessageId,
                seq,
                ChatRedisEventType.TOKEN,
                content,
                null,
                null,
                null
        );
    }

    public static ChatRedisEvent done(
            Long userMessageId,
            long seq,
            String content,
            List<RecommendedProductResponse> products
    ) {
        return new ChatRedisEvent(
                userMessageId,
                seq,
                ChatRedisEventType.DONE,
                content,
                products,
                null,
                null
        );
    }

    public static ChatRedisEvent error(
            Long userMessageId,
            long seq,
            String code,
            String message
    ) {
        return new ChatRedisEvent(
                userMessageId,
                seq,
                ChatRedisEventType.ERROR,
                null,
                null,
                code,
                message
        );
    }
}
