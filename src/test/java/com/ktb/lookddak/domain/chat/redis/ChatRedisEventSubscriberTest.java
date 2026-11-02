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
    @DisplayName("다른 BE가 발행한 token을 현재 JVM의 SSE emitter로 전달한다")
    void forwardTokenToLocalEmitter() throws Exception {
        Message message = mock(Message.class);
        when(message.getBody()).thenReturn("event".getBytes());
        when(objectMapper.readValue(any(byte[].class), eq(ChatRedisEvent.class)))
                .thenReturn(ChatRedisEvent.token(4365L, 7L, "추천"));

        subscriber.onMessage(message, "pattern".getBytes());

        verify(chatSseService).sendToken(4365L, "추천");
    }

    @Test
    @DisplayName("완료 이벤트는 현재 JVM의 SSE 연결에 done을 전달한 뒤 종료한다")
    void forwardDoneAndCompleteLocalEmitter() throws Exception {
        Message message = mock(Message.class);
        when(message.getBody()).thenReturn("event".getBytes());
        when(objectMapper.readValue(any(byte[].class), eq(ChatRedisEvent.class)))
                .thenReturn(ChatRedisEvent.done(4365L, 8L, "추천 결과", java.util.List.of()));

        subscriber.onMessage(message, "pattern".getBytes());

        verify(chatSseService).send(eq(4365L), eq("done"), any());
        verify(chatSseService).complete(4365L);
    }

    @Test
    @DisplayName("실패 이벤트는 현재 JVM의 SSE 연결에 error를 전달한 뒤 종료한다")
    void forwardErrorAndCompleteLocalEmitter() throws Exception {
        Message message = mock(Message.class);
        when(message.getBody()).thenReturn("event".getBytes());
        when(objectMapper.readValue(any(byte[].class), eq(ChatRedisEvent.class)))
                .thenReturn(ChatRedisEvent.error(4365L, 8L, "AI_TIMEOUT", "시간 초과"));

        subscriber.onMessage(message, "pattern".getBytes());

        verify(chatSseService).send(eq(4365L), eq("error"), any());
        verify(chatSseService).complete(4365L);
    }
}
