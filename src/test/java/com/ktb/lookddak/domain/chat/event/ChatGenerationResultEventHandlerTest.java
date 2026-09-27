package com.ktb.lookddak.domain.chat.event;

import com.ktb.lookddak.domain.chat.service.ChatGenerationResultService;
import com.ktb.lookddak.global.client.ai.dto.AiChatDoneResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatGenerationResultEventHandlerTest {

    @Mock
    private ChatGenerationResultService resultService;

    @InjectMocks
    private ChatGenerationResultEventHandler handler;

    @Test
    @DisplayName("생성 성공 이벤트를 결과 저장 서비스에 전달한다")
    void handleSucceededEvent() {
        AiChatDoneResponse response = new AiChatDoneResponse(
                123L,
                "최종 응답",
                List.of()
        );

        handler.handleSucceeded(new ChatGenerationSucceededEvent(
                501L,
                response
        ));

        verify(resultService).complete(501L, response);
    }

    @Test
    @DisplayName("생성 실패 이벤트를 실패 처리 서비스에 전달한다")
    void handleFailedEvent() {
        handler.handleFailed(new ChatGenerationFailedEvent(
                501L,
                "AI_RESPONSE_TIMEOUT",
                "AI 응답 제한 시간을 초과했습니다."
        ));

        verify(resultService).fail(501L);
    }
}
