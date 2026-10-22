package com.ktb.lookddak.domain.chat.event;

import com.ktb.lookddak.global.client.ai.dto.AiChatDoneResponse;
import lombok.Getter;

@Getter
public class ChatV2GenerationSucceededEvent {

    private final Long userMessageId;
    private final AiChatDoneResponse response;

    public ChatV2GenerationSucceededEvent(
            Long userMessageId,
            AiChatDoneResponse response
    ) {
        this.userMessageId = userMessageId;
        this.response = response;
    }
}
