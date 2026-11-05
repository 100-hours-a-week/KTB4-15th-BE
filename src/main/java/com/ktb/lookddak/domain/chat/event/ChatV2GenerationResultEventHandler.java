package com.ktb.lookddak.domain.chat.event;

import com.ktb.lookddak.domain.chat.dto.ChatSseDoneResponse;
import com.ktb.lookddak.domain.chat.dto.ChatSseErrorResponse;
import com.ktb.lookddak.domain.chat.service.ChatGenerationResultService;
import com.ktb.lookddak.domain.chat.service.ChatSseService;
import com.ktb.lookddak.global.client.ai.config.AiResultPersistenceExecutorMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.RejectedExecutionException;

@Slf4j
@Component
public class ChatV2GenerationResultEventHandler {

    private static final String RESULT_PERSISTENCE_FAILED =
            "AI_RESULT_PERSISTENCE_FAILED";
    private static final String RESULT_PERSISTENCE_REJECTED =
            "AI_RESULT_PERSISTENCE_REJECTED";

    private final ChatGenerationResultService resultService;
    private final ChatSseService chatSseService;
    private final ThreadPoolTaskExecutor resultPersistenceExecutor;
    private final AiResultPersistenceExecutorMetrics executorMetrics;

    public ChatV2GenerationResultEventHandler(
            ChatGenerationResultService resultService,
            ChatSseService chatSseService,
            @Qualifier("aiResultPersistenceExecutor")
            ThreadPoolTaskExecutor resultPersistenceExecutor,
            AiResultPersistenceExecutorMetrics executorMetrics
    ) {
        this.resultService = resultService;
        this.chatSseService = chatSseService;
        this.resultPersistenceExecutor = resultPersistenceExecutor;
        this.executorMetrics = executorMetrics;
    }

    /**
     * AI 통신 스레드가 아닌 결과 저장 Executor에서 DB 저장을 수행한다.
     * 저장이 끝난 뒤에만 프런트에 done을 전송하므로 done을 받은 시점에는 조회 API도 완료 상태를 반환한다.
     */
    @EventListener
    public void handleSucceeded(ChatV2GenerationSucceededEvent event) {
        submit(event.getUserMessageId(), () -> persistSucceeded(event));
    }

    private void persistSucceeded(ChatV2GenerationSucceededEvent event) {
        try {
            ChatSseDoneResponse response = resultService.complete(
                    event.getUserMessageId(), event.getResponse()
            );
            if (response != null) {
                chatSseService.send(
                        event.getUserMessageId(),
                        "done",
                        response
                );
            }
        } catch (Exception exception) {
            log.error(
                    "V2 AI chat result persistence failed userMessageId={}",
                    event.getUserMessageId(),
                    exception
            );
            resultService.fail(event.getUserMessageId());
            chatSseService.send(
                    event.getUserMessageId(),
                    "error",
                    new ChatSseErrorResponse(
                            event.getUserMessageId(),
                            RESULT_PERSISTENCE_FAILED,
                            "AI 응답 결과를 저장하지 못했습니다."
                    )
            );
        } finally {
            chatSseService.complete(event.getUserMessageId());
        }
    }

    @EventListener
    public void handleFailed(ChatV2GenerationFailedEvent event) {
        submit(event.getUserMessageId(), () -> persistFailed(event));
    }

    private void persistFailed(ChatV2GenerationFailedEvent event) {
        try {
            resultService.fail(event.getUserMessageId());
        } catch (Exception exception) {
            log.error(
                    "V2 AI chat failure status update failed userMessageId={}",
                    event.getUserMessageId(),
                    exception
            );
        } finally {
            chatSseService.send(
                    event.getUserMessageId(),
                    "error",
                    new ChatSseErrorResponse(
                            event.getUserMessageId(),
                            event.getCode(),
                            event.getMessage()
                    )
            );
            chatSseService.complete(event.getUserMessageId());
        }
    }

    private void submit(Long userMessageId, Runnable task) {
        try {
            resultPersistenceExecutor.execute(task);
        } catch (RejectedExecutionException exception) {
            executorMetrics.rejected();
            log.error(
                    "AI result persistence task rejected userMessageId={}",
                    userMessageId,
                    exception
            );
            failRejectedResult(userMessageId);
        }
    }

    /**
     * 결과 저장 작업을 더 이상 대기열에 넣을 수 없으면, 유실시키지 않고
     * 현재 요청만 명시적으로 실패·종료한다. 이 경로는 과부하 상황에서만
     * 실행되므로 상태 갱신과 SSE 정리를 호출 스레드에서 즉시 수행한다.
     */
    private void failRejectedResult(Long userMessageId) {
        try {
            resultService.fail(userMessageId);
        } catch (Exception exception) {
            log.error(
                    "Failed to mark rejected AI result as failed userMessageId={}",
                    userMessageId,
                    exception
            );
        } finally {
            chatSseService.send(
                    userMessageId,
                    "error",
                    new ChatSseErrorResponse(
                            userMessageId,
                            RESULT_PERSISTENCE_REJECTED,
                            "요청이 많아 응답 결과를 처리하지 못했습니다. 다시 시도해 주세요."
                    )
            );
            chatSseService.complete(userMessageId);
        }
    }

}
