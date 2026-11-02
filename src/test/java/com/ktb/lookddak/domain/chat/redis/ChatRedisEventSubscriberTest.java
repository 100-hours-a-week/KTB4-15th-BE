package com.ktb.lookddak.domain.chat.redis;

import com.ktb.lookddak.domain.chat.service.ChatSseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
}
