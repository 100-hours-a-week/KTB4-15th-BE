package com.ktb.lookddak.domain.chat.event;

import com.ktb.lookddak.domain.chat.service.ChatSseService;
import com.ktb.lookddak.global.client.ai.chat.AiChatStreamEvent;
import com.ktb.lookddak.global.client.ai.chat.AiChatStreamEventType;
import com.ktb.lookddak.global.client.ai.chat.AiChatStreamingClient;
import com.ktb.lookddak.global.client.ai.exception.AiChatException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatV2GenerationStreamListener {

    private final AiChatStreamingClient aiChatStreamingClient;
    private final ChatSseService chatSseService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * DB의 GENERATING 상태가 커밋된 이후에만 AI SSE 연결을 시작한다.
     * subscribe()는 응답을 기다려 블로킹하지 않고, AI 이벤트가 도착할 때마다 아래 콜백을 실행한다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ChatV2GenerationRequestedEvent event) {
        aiChatStreamingClient.stream(event.toAiRequest())
                .subscribe(
                        streamEvent -> forward(event.getUserMessageId(), streamEvent),
                        exception -> sendError(event.getUserMessageId(), exception)
                );
    }

    private void forward(Long userMessageId, AiChatStreamEvent streamEvent) {
        if (streamEvent.getType() == AiChatStreamEventType.TOKEN) {
            chatSseService.sendToken(
                    userMessageId,
                    streamEvent.getContent()
            );
            return;
        }

        if (streamEvent.getType() == AiChatStreamEventType.DONE) {
            eventPublisher.publishEvent(new ChatV2GenerationSucceededEvent(
                    userMessageId,
                    streamEvent.getDoneResponse()
            ));
        }
    }

    private void sendError(Long userMessageId, Throwable throwable) {
        if (throwable instanceof AiChatException exception) {
            log.warn(
                    "V2 AI chat stream failed userMessageId={} code={}",
                    userMessageId,
                    exception.getCode(),
                    exception
            );
            eventPublisher.publishEvent(new ChatV2GenerationFailedEvent(
                    userMessageId,
                    exception.getCode(),
                    exception.getMessage()
            ));
        } else {
            log.error("V2 AI chat stream failed userMessageId={}", userMessageId, throwable);
            eventPublisher.publishEvent(new ChatV2GenerationFailedEvent(
                    userMessageId,
                    "AI_UNEXPECTED_ERROR",
                    "AI 응답 생성 중 오류가 발생했습니다."
            ));
        }
    }
}
