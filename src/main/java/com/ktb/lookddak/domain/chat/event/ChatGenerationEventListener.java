package com.ktb.lookddak.domain.chat.event;

import com.ktb.lookddak.global.client.ai.chat.AiChatClient;
import com.ktb.lookddak.global.client.ai.config.AiTaskExecutorMonitor;
import com.ktb.lookddak.global.client.ai.dto.AiChatDoneResponse;
import com.ktb.lookddak.global.client.ai.exception.AiChatException;
import com.ktb.lookddak.global.client.ai.exception.AiFailureClassifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ChatGenerationEventListener {

    private final AiChatClient aiChatClient;
    private final ApplicationEventPublisher eventPublisher;
    private final Executor chatTaskExecutor;
    private final AiTaskExecutorMonitor executorMonitor;

    public ChatGenerationEventListener(
            AiChatClient aiChatClient,
            ApplicationEventPublisher eventPublisher,
            @Qualifier("chatTaskExecutor") Executor chatTaskExecutor,
            AiTaskExecutorMonitor executorMonitor
    ) {
        this.aiChatClient = aiChatClient;
        this.eventPublisher = eventPublisher;
        this.chatTaskExecutor = chatTaskExecutor;
        this.executorMonitor = executorMonitor;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ChatGenerationRequestedEvent event) {
        long submittedAtNanos = System.nanoTime();
        try {
            chatTaskExecutor.execute(
                    () -> generate(event, submittedAtNanos)
            );
            executorMonitor.logStatus("chat", chatTaskExecutor);
        } catch (RejectedExecutionException exception) {
            log.warn(
                    "AI chat task rejected. chatRoomId={}, userMessageId={}",
                    event.getChatRoomId(),
                    event.getUserMessageId()
            );
            executorMonitor.logStatus("chat", chatTaskExecutor);
            publishFailure(
                    event,
                    "AI_TASK_REJECTED",
                    "AI 채팅 요청이 많아 작업을 시작하지 못했습니다."
            );
        }
    }

    private void generate(
            ChatGenerationRequestedEvent event,
            long submittedAtNanos
    ) {
        long startedAtNanos = System.nanoTime();
        log.info(
                "AI_CHAT_STARTED chatRoomId={} userMessageId={} queueWaitMs={}",
                event.getChatRoomId(),
                event.getUserMessageId(),
                elapsedMillis(submittedAtNanos, startedAtNanos)
        );
        try {
            AiChatDoneResponse response = aiChatClient.requestChat(
                    event.toAiRequest()
            );
            eventPublisher.publishEvent(new ChatGenerationSucceededEvent(
                    event.getUserMessageId(),
                    response
            ));
            log.info(
                    "AI_CHAT_COMPLETED chatRoomId={} userMessageId={} elapsedMs={} totalElapsedMs={}",
                    event.getChatRoomId(),
                    event.getUserMessageId(),
                    elapsedMillis(startedAtNanos, System.nanoTime()),
                    elapsedMillis(submittedAtNanos, System.nanoTime())
            );
        } catch (AiChatException exception) {
            logAiFailure(event, exception, startedAtNanos);
            publishFailure(event, exception.getCode(), exception.getMessage());
        } catch (Exception exception) {
            log.error(
                    "AI_CHAT_FAILED chatRoomId={} userMessageId={} code={} elapsedMs={}",
                    event.getChatRoomId(),
                    event.getUserMessageId(),
                    "AI_UNEXPECTED_ERROR",
                    elapsedMillis(startedAtNanos, System.nanoTime()),
                    exception
            );
            publishFailure(
                    event,
                    "AI_UNEXPECTED_ERROR",
                    "AI 응답 생성 중 예상하지 못한 오류가 발생했습니다."
            );
        } finally {
            executorMonitor.logStatus("chat", chatTaskExecutor);
        }
    }

    private void logAiFailure(
            ChatGenerationRequestedEvent event,
            AiChatException exception,
            long startedAtNanos
    ) {
        if (AiFailureClassifier.requiresIncidentAlert(exception.getCode())) {
            log.error(
                    "AI_CHAT_FAILED chatRoomId={} userMessageId={} code={} elapsedMs={}",
                    event.getChatRoomId(),
                    event.getUserMessageId(),
                    exception.getCode(),
                    elapsedMillis(startedAtNanos, System.nanoTime()),
                    exception
            );
            return;
        }

        log.warn(
                "AI_CHAT_FAILED chatRoomId={} userMessageId={} code={} elapsedMs={}",
                event.getChatRoomId(),
                event.getUserMessageId(),
                exception.getCode(),
                elapsedMillis(startedAtNanos, System.nanoTime()),
                exception
        );
    }

    private long elapsedMillis(long startNanos, long endNanos) {
        return TimeUnit.NANOSECONDS.toMillis(endNanos - startNanos);
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
