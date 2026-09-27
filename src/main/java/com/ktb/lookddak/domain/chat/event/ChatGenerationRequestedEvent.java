package com.ktb.lookddak.domain.chat.event;

import com.ktb.lookddak.domain.chat.entity.ChatSourceType;
import com.ktb.lookddak.global.client.ai.dto.AiChatRequest;
import lombok.Getter;

import java.util.List;

@Getter
public class ChatGenerationRequestedEvent {

    private final Long memberId;
    private final Long chatRoomId;
    private final Long userMessageId;
    private final String content;
    private final ChatSourceType sourceType;
    private final List<String> productIds;

    public ChatGenerationRequestedEvent(
            Long memberId,
            Long chatRoomId,
            Long userMessageId,
            String content,
            ChatSourceType sourceType,
            List<String> productIds
    ) {
        this.memberId = memberId;
        this.chatRoomId = chatRoomId;
        this.userMessageId = userMessageId;
        this.content = content;
        this.sourceType = sourceType;
        this.productIds = productIds == null
                ? List.of()
                : List.copyOf(productIds);
    }

    public AiChatRequest toAiRequest() {
        return new AiChatRequest(
                chatRoomId,
                memberId,
                content,
                sourceType,
                productIds
        );
    }
}
