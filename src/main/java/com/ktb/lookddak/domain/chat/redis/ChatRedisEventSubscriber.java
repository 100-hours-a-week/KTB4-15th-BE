package com.ktb.lookddak.domain.chat.redis;

import com.ktb.lookddak.domain.chat.service.ChatSseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

/**
 * 모든 BE 인스턴스가 Redis Pub/Sub event를 수신해 자기 JVM의 SSE emitter에 전달한다.
 *
 * <p>token, done, error를 기존 SSE event payload 형식으로 복원한다. 재연결 Snapshot
 * 복구는 후속 단계에서 connect 경로에 추가한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRedisEventSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final ChatSseService chatSseService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            ChatRedisEvent event = objectMapper.readValue(
                    message.getBody(),
                    ChatRedisEvent.class
            );
            forward(event);
        } catch (JacksonException exception) {
            log.error(
                    "Failed to deserialize Redis chat event channel={}",
                    new String(message.getChannel(), StandardCharsets.UTF_8),
                    exception
            );
        }
    }

    private void forward(ChatRedisEvent event) {
        chatSseService.handleRedisEvent(event);
    }
}
