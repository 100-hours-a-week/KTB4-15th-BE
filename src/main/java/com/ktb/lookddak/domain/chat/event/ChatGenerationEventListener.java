package com.ktb.lookddak.domain.chat.event;

import com.ktb.lookddak.global.client.ai.chat.AiChatClient;
import com.ktb.lookddak.global.client.ai.dto.AiChatDoneResponse;
import com.ktb.lookddak.global.client.ai.exception.AiChatException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatGenerationEventListener {

    private final AiChatClient aiChatClient;
    private final ApplicationEventPublisher eventPublisher;

    @Async("aiTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ChatGenerationRequestedEvent event) {
        try {
            AiChatDoneResponse response = aiChatClient.requestChat(
                    event.toAiRequest()
            );
            eventPublisher.publishEvent(new ChatGenerationSucceededEvent(
                    event.getUserMessageId(),
                    response
            ));
        } catch (AiChatException exception) {
            log.warn(
                    "AI chat generation failed. chatRoomId={}, userMessageId={}, code={}",
                    event.getChatRoomId(),
                    event.getUserMessageId(),
                    exception.getCode(),
                    exception
            );
            publishFailure(event, exception.getCode(), exception.getMessage());
        } catch (Exception exception) {
            log.error(
                    "Unexpected AI chat generation error. chatRoomId={}, userMessageId={}",
                    event.getChatRoomId(),
                    event.getUserMessageId(),
                    exception
            );
            publishFailure(
                    event,
                    "AI_UNEXPECTED_ERROR",
                    "AI 응답 생성 중 예상하지 못한 오류가 발생했습니다."
            );
        }
    }

    private void publishFailure(
            ChatGenerationRequestedEvent event,
            String code,
            String message
    ) {
        eventPublisher.publishEvent(new ChatGenerationFailedEvent(
                event.getUserMessageId(),
                code,
                message
        ));
    }
}
