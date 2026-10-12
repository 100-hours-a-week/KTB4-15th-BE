package com.ktb.lookddak.domain.chat.event;

import com.ktb.lookddak.domain.chat.dto.ChatSseProductsResponse;
import com.ktb.lookddak.domain.chat.service.ChatSseService;
import com.ktb.lookddak.global.client.ai.chat.AiChatStreamEvent;
import com.ktb.lookddak.global.client.ai.chat.AiChatStreamingClient;
import com.ktb.lookddak.global.client.ai.dto.AiChatDoneResponse;
import com.ktb.lookddak.global.client.ai.dto.AiChatProductsResponse;
import com.ktb.lookddak.global.client.ai.dto.AiChatRequest;
import com.ktb.lookddak.global.client.ai.dto.AiChatTokenResponse;
import com.ktb.lookddak.global.client.ai.dto.AiRecommendedProductResponse;
import com.ktb.lookddak.global.client.ai.exception.AiChatException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatV2GenerationStreamListenerTest {

    @Mock
    private AiChatStreamingClient aiChatStreamingClient;

    @Mock
    private ChatSseService chatSseService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("AI token, products, done 이벤트를 프런트 SSE 이벤트로 전달한다")
    void forwardAiEventsToSse() {
        ChatV2GenerationStreamListener listener = new ChatV2GenerationStreamListener(
                aiChatStreamingClient,
                chatSseService,
                eventPublisher
        );
        given(aiChatStreamingClient.stream(any(AiChatRequest.class)))
                .willReturn(Flux.just(
                        AiChatStreamEvent.token(new AiChatTokenResponse(10L, "추천을 찾고 있어요.")),
                        AiChatStreamEvent.products(new AiChatProductsResponse(
                                10L,
                                List.of(new AiRecommendedProductResponse(
                                        "0000001",
                                        "오버핏 코튼 셔츠",
                                        "https://example.com/1.jpg",
                                        "https://shop.example.com/1",
                                        "NAVY",
                                        com.ktb.lookddak.domain.product.entity.ProductItemType.TOP,
                                        69_000,
                                        "소개팅 룩에 잘 어울립니다."
                                ))
                        )),
                        AiChatStreamEvent.done(new AiChatDoneResponse(
                                10L,
                                "추천 결과입니다.",
                                List.of()
                        ))
                ));

        listener.handle(event());

        ArgumentCaptor<Object> dataCaptor = ArgumentCaptor.forClass(Object.class);
        verify(chatSseService).send(eq(100L), eq("token"), dataCaptor.capture());
        assertThat(dataCaptor.getValue()).hasFieldOrPropertyWithValue(
                "content",
                "추천을 찾고 있어요."
        );
        ArgumentCaptor<Object> productsCaptor = ArgumentCaptor.forClass(Object.class);
        verify(chatSseService).send(eq(100L), eq("products"), productsCaptor.capture());
        assertThat(productsCaptor.getValue()).hasFieldOrPropertyWithValue(
                "userMessageId",
                100L
        );
        ChatSseProductsResponse productsResponse =
                (ChatSseProductsResponse) productsCaptor.getValue();
        assertThat(productsResponse.getProducts())
                .singleElement()
                .satisfies(product -> {
                    assertThat(product.getProductCode()).isEqualTo("0000001");
                    assertThat(product.getItemType())
                            .isEqualTo(com.ktb.lookddak.domain.product.entity.ProductItemType.TOP);
                });
        verify(eventPublisher).publishEvent(any(ChatV2GenerationSucceededEvent.class));
    }

    @Test
    @DisplayName("AI 스트림 오류는 error SSE 이벤트로 전달하고 연결을 종료한다")
    void forwardAiErrorToSse() {
        ChatV2GenerationStreamListener listener = new ChatV2GenerationStreamListener(
                aiChatStreamingClient,
                chatSseService,
                eventPublisher
        );
        given(aiChatStreamingClient.stream(any(AiChatRequest.class)))
                .willReturn(Flux.error(new AiChatException(
                        "SERVER_BUSY",
                        "AI 서버가 혼잡합니다.",
                        10L
                )));

        listener.handle(event());

        verify(eventPublisher).publishEvent(any(ChatV2GenerationFailedEvent.class));
    }

    private ChatV2GenerationRequestedEvent event() {
        return new ChatV2GenerationRequestedEvent(
                1L,
                10L,
                100L,
                "가을 니트를 추천해줘",
                com.ktb.lookddak.domain.chat.entity.ChatSourceType.GENERAL,
                List.of()
        );
    }
}
