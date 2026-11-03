package com.ktb.lookddak.domain.chat.event;

import com.ktb.lookddak.domain.chat.dto.ChatSseDoneResponse;
import com.ktb.lookddak.domain.chat.service.ChatGenerationResultService;
import com.ktb.lookddak.domain.chat.service.ChatSseService;
import com.ktb.lookddak.global.client.ai.config.AiResultPersistenceExecutorMetrics;
import com.ktb.lookddak.global.client.ai.dto.AiChatDoneResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatV2GenerationResultEventHandlerTest {

    @Mock
    private ChatGenerationResultService resultService;

    @Mock
    private ChatSseService chatSseService;

    @Mock
    private AiResultPersistenceExecutorMetrics executorMetrics;

    @Test
    @DisplayName("결과 저장에 성공하면 done 이벤트를 전송한 뒤 SSE 연결을 종료한다")
    void saveResultBeforeSendingDone() {
        ChatV2GenerationResultEventHandler handler = handler();
        ChatV2GenerationSucceededEvent event = new ChatV2GenerationSucceededEvent(
                100L,
                new AiChatDoneResponse(10L, "추천 결과입니다.", List.of())
        );
        given(resultService.complete(100L, event.getResponse()))
                .willReturn(new ChatSseDoneResponse(
                        100L,
                        "추천 결과입니다.",
                        List.of()
                ));

        handler.handleSucceeded(event);

        verify(resultService).complete(100L, event.getResponse());
        verify(chatSseService).send(eq(100L), eq("done"), any());
        verify(chatSseService).complete(100L);
    }

    @Test
    @DisplayName("결과 저장에 실패하면 FAILED 처리 후 error 이벤트를 전송한다")
    void failGenerationWhenResultPersistenceFails() {
        ChatV2GenerationResultEventHandler handler = handler();
        ChatV2GenerationSucceededEvent event = new ChatV2GenerationSucceededEvent(
                100L,
                new AiChatDoneResponse(10L, "추천 결과입니다.", List.of())
        );
        willThrow(new IllegalStateException("저장 실패"))
                .given(resultService)
                .complete(100L, event.getResponse());

        handler.handleSucceeded(event);

        verify(resultService).fail(100L);
        verify(chatSseService).send(eq(100L), eq("error"), any());
        verify(chatSseService).complete(100L);
    }

    @Test
    @DisplayName("AI 통신 실패 시 FAILED 처리 후 error 이벤트를 전송한다")
    void failGenerationBeforeSendingError() {
        ChatV2GenerationResultEventHandler handler = handler();
        ChatV2GenerationFailedEvent event = new ChatV2GenerationFailedEvent(
                100L,
                "AI_RESPONSE_TIMEOUT",
                "AI 응답 제한 시간을 초과했습니다."
        );

        handler.handleFailed(event);

        verify(resultService).fail(100L);
        verify(chatSseService).send(eq(100L), eq("error"), any());
        verify(chatSseService).complete(100L);
    }

    @Test
    @DisplayName("결과 저장 대기열이 가득 차면 요청을 FAILED 처리하고 SSE 연결을 정리한다")
    void failAndCleanUpWhenResultPersistenceTaskIsRejected() {
        ThreadPoolTaskExecutor rejectedExecutor = mock(ThreadPoolTaskExecutor.class);
        doThrow(new RejectedExecutionException("queue full"))
                .when(rejectedExecutor)
                .execute(any(Runnable.class));
        ChatV2GenerationResultEventHandler handler = handler(rejectedExecutor);
        ChatV2GenerationSucceededEvent event = new ChatV2GenerationSucceededEvent(
                100L,
                new AiChatDoneResponse(10L, "추천 결과입니다.", List.of())
        );

        handler.handleSucceeded(event);

        verify(executorMetrics).rejected();
        verify(resultService).fail(100L);
        verify(chatSseService).send(eq(100L), eq("error"), any());
        verify(chatSseService).complete(100L);
    }

    private ChatV2GenerationResultEventHandler handler() {
        ThreadPoolTaskExecutor directExecutor = mock(ThreadPoolTaskExecutor.class);
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(directExecutor).execute(any(Runnable.class));
        return handler(directExecutor);
    }

    private ChatV2GenerationResultEventHandler handler(
            ThreadPoolTaskExecutor executor
    ) {
        return new ChatV2GenerationResultEventHandler(
                resultService,
                chatSseService,
                executor,
                executorMetrics
        );
    }
}
