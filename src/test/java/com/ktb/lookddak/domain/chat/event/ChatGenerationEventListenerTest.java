package com.ktb.lookddak.domain.chat.event;

import com.ktb.lookddak.domain.chat.entity.ChatSourceType;
import com.ktb.lookddak.global.client.ai.chat.AiChatClient;
import com.ktb.lookddak.global.client.ai.dto.AiChatDoneResponse;
import com.ktb.lookddak.global.client.ai.dto.AiChatRequest;
import com.ktb.lookddak.global.client.ai.exception.AiChatException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatGenerationEventListenerTest {

    @Mock
    private AiChatClient aiChatClient;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ChatGenerationEventListener listener;

    @Test
    @DisplayName("AI done 응답을 받으면 생성 성공 이벤트를 발행한다")
    void publishSucceededEvent() {
        ChatGenerationRequestedEvent requestedEvent = requestedEvent();
        AiChatDoneResponse doneResponse = new AiChatDoneResponse(
                123L,
                "최종 AI 응답",
                List.of()
        );
        given(aiChatClient.requestChat(any(AiChatRequest.class)))
                .willReturn(doneResponse);

        listener.handle(requestedEvent);

        ArgumentCaptor<ChatGenerationSucceededEvent> captor =
                ArgumentCaptor.forClass(ChatGenerationSucceededEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getUserMessageId()).isEqualTo(501L);
        assertThat(captor.getValue().getResponse()).isSameAs(doneResponse);
    }

    @Test
    @DisplayName("AI 통신 예외가 발생하면 생성 실패 이벤트를 발행한다")
    void publishFailedEventForAiException() {
        ChatGenerationRequestedEvent requestedEvent = requestedEvent();
        given(aiChatClient.requestChat(any(AiChatRequest.class)))
                .willThrow(new AiChatException(
                        "recommendation_search_failed",
                        "상품 추천 처리 중 오류가 발생했습니다.",
                        123L
                ));

        listener.handle(requestedEvent);

        ArgumentCaptor<ChatGenerationFailedEvent> captor =
                ArgumentCaptor.forClass(ChatGenerationFailedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getUserMessageId()).isEqualTo(501L);
        assertThat(captor.getValue().getCode())
                .isEqualTo("recommendation_search_failed");
    }

    @Test
    @DisplayName("예상하지 못한 오류도 공통 생성 실패 이벤트로 변환한다")
    void publishFailedEventForUnexpectedException() {
        given(aiChatClient.requestChat(any(AiChatRequest.class)))
                .willThrow(new IllegalStateException("unexpected"));

        listener.handle(requestedEvent());

        ArgumentCaptor<ChatGenerationFailedEvent> captor =
                ArgumentCaptor.forClass(ChatGenerationFailedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getCode())
                .isEqualTo("AI_UNEXPECTED_ERROR");
    }

    @Test
    @DisplayName("생성 요청 이벤트를 AI 채팅 요청 DTO로 변환한다")
    void convertRequestedEventToAiRequest() {
        AiChatRequest request = requestedEvent().toAiRequest();

        assertThat(request.getChatId()).isEqualTo(123L);
        assertThat(request.getUserId()).isEqualTo(1L);
        assertThat(request.getMessage()).isEqualTo("네이비 셔츠 추천해줘");
        assertThat(request.getSourceType()).isEqualTo(ChatSourceType.GENERAL);
        assertThat(request.getProductIds()).containsExactly("0000001");
    }

    private ChatGenerationRequestedEvent requestedEvent() {
        return new ChatGenerationRequestedEvent(
                1L,
                123L,
                501L,
                "네이비 셔츠 추천해줘",
                ChatSourceType.GENERAL,
                List.of("0000001")
        );
    }
}
