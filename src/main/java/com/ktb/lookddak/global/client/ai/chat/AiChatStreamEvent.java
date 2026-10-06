package com.ktb.lookddak.global.client.ai.chat;

import com.ktb.lookddak.global.client.ai.dto.AiChatDoneResponse;
import com.ktb.lookddak.global.client.ai.dto.AiChatProductResponse;
import com.ktb.lookddak.global.client.ai.dto.AiChatTokenResponse;
import lombok.Getter;

@Getter
public class AiChatStreamEvent {

    private final AiChatStreamEventType type;
    private final Long chatId;
    private final String content;
    private final AiChatProductResponse productResponse;
    private final AiChatDoneResponse doneResponse;

    private AiChatStreamEvent(
            AiChatStreamEventType type,
            Long chatId,
            String content,
            AiChatProductResponse productResponse,
            AiChatDoneResponse doneResponse
    ) {
        this.type = type;
        this.chatId = chatId;
        this.content = content;
        this.productResponse = productResponse;
        this.doneResponse = doneResponse;
    }

    public static AiChatStreamEvent token(AiChatTokenResponse response) {
        return new AiChatStreamEvent(
                AiChatStreamEventType.TOKEN,
                response.getChatId(),
                response.getContent(),
                null,
                null
        );
    }

    public static AiChatStreamEvent product(AiChatProductResponse response) {
        return new AiChatStreamEvent(
                AiChatStreamEventType.PRODUCT,
                response.getChatId(),
                null,
                response,
                null
        );
    }

    public static AiChatStreamEvent done(AiChatDoneResponse response) {
        return new AiChatStreamEvent(
                AiChatStreamEventType.DONE,
                response.getChatId(),
                response.getContent(),
                null,
                response
        );
    }

    public boolean isDone() {
        return type == AiChatStreamEventType.DONE;
    }
}
