package com.ktb.lookddak.domain.chat.event;

import com.ktb.lookddak.domain.chat.dto.ChatSseDoneResponse;
import com.ktb.lookddak.domain.chat.dto.ChatSseErrorResponse;
import com.ktb.lookddak.domain.chat.service.ChatGenerationResultService;
import com.ktb.lookddak.domain.chat.service.ChatSseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatV2GenerationResultEventHandler {

    private static final String RESULT_PERSISTENCE_FAILED =
            "AI_RESULT_PERSISTENCE_FAILED";

    private final ChatGenerationResultService resultService;
    private final ChatSseService chatSseService;

    /**
     * AI 통신 스레드가 아닌 결과 저장 Executor에서 DB 저장을 수행한다.
     * 저장이 끝난 뒤에만 프런트에 done을 전송하므로 done을 받은 시점에는 조회 API도 완료 상태를 반환한다.
     */
    @Async("aiResultPersistenceExecutor")
    @EventListener
    public void handleSucceeded(ChatV2GenerationSucceededEvent event) {
        try {
            ChatSseDoneResponse response = resultService.complete(
                    event.getUserMessageId(),
                    event.getResponse()
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

    @Async("aiResultPersistenceExecutor")
    @EventListener
    public void handleFailed(ChatV2GenerationFailedEvent event) {
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
}
