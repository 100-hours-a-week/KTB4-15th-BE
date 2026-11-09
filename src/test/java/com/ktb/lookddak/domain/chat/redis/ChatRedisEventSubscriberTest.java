package com.ktb.lookddak.domain.chat.redis;

import com.ktb.lookddak.domain.chat.service.ChatSseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.data.redis.connection.Message;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatRedisEventSubscriberTest {

    private final ObjectMapper objectMapper = mock(ObjectMapper.class);
    private final ChatSseService chatSseService = mock(ChatSseService.class);
    private final ChatRedisEventSubscriber subscriber = new ChatRedisEventSubscriber(
            objectMapper,
            chatSseService
    );

    @Test
    @DisplayName("다른 BE가 발행한 event를 SSE 복구 상태를 가진 서비스로 전달한다")
    void forwardEventToSseService() throws Exception {
        Message message = mock(Message.class);
        when(message.getBody()).thenReturn("event".getBytes());
        when(objectMapper.readValue(any(byte[].class), eq(ChatRedisEvent.class)))
                .thenReturn(ChatRedisEvent.token(4365L, 7L, "추천"));

        subscriber.onMessage(message, "pattern".getBytes());

        verify(chatSseService).handleRedisEvent(ChatRedisEvent.token(4365L, 7L, "추천"));
    }
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("상품이 포함된 DONE JSON을 실제로 복원하여 수신 Pod의 SSE 서비스에 전달한다")
    void forwardDoneWithProductsUsingRealJson(boolean selected) throws Exception {
        ObjectMapper mapper = JsonMapper.builder().build();
        String json = """
                {"userMessageId":4365,"seq":8,"type":"DONE","content":"추천 상품입니다",
                 "products":[{"productId":10,"productName":"니트","productImageUrl":"https://example.com/a.jpg",
                 "currentPrice":25000,"color":"BLACK","itemType":"TOP","purchaseUrl":"https://example.com/p",
                 "recommendedReason":"가을에 적합합니다","isWishlisted":%s,"wishlistId":%s,
                 "isFittingCandidate":%s,"fittingCandidateId":%s}],"code":null,"message":null}
                """.formatted(selected, selected ? "20" : "null", selected, selected ? "30" : "null");
        ChatRedisEvent expected = mapper.readValue(json, ChatRedisEvent.class);
        var product = expected.products().getFirst();
        assertThat(product.isWishlisted()).isEqualTo(selected);
        assertThat(product.isFittingCandidate()).isEqualTo(selected);
        assertThat(product.getWishlistId()).isEqualTo(selected ? 20L : null);
        assertThat(product.getFittingCandidateId()).isEqualTo(selected ? 30L : null);

        var template = mock(StringRedisTemplate.class);
        var keys = new ChatRedisKeyFactory(new ChatRedisProperties(true, "lookddak:chat"));
        new ChatRedisEventPublisher(template, mapper, keys).publish(expected);
        var payload = ArgumentCaptor.forClass(String.class);
        verify(template).convertAndSend(eq(keys.eventChannel(4365L)), payload.capture());

        Message message = mock(Message.class);
        when(message.getBody()).thenReturn(payload.getValue().getBytes(StandardCharsets.UTF_8));
        new ChatRedisEventSubscriber(mapper, chatSseService).onMessage(message, null);

        var received = ArgumentCaptor.forClass(ChatRedisEvent.class);
        verify(chatSseService).handleRedisEvent(received.capture());
        assertThat(received.getValue())
                .usingRecursiveComparison().isEqualTo(expected);
    }
}
