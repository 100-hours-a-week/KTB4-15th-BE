package com.ktb.lookddak.domain.chat.redis;

import com.ktb.lookddak.domain.chat.dto.ChatSseDoneResponse;
import com.ktb.lookddak.domain.chat.dto.ChatSseErrorResponse;
import com.ktb.lookddak.domain.chat.dto.ChatSseTokenResponse;
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
        if (event.type() == ChatRedisEventType.TOKEN) {
            chatSseService.sendToken(
                    event.userMessageId(),
                    event.content()
            );
            return;
        }

        if (event.type() == ChatRedisEventType.DONE) {
            chatSseService.send(
                    event.userMessageId(),
                    "done",
                    new ChatSseDoneResponse(
                            event.userMessageId(),
                            event.content(),
                            event.products()
                    )
            );
            chatSseService.complete(event.userMessageId());
            return;
        }

        if (event.type() == ChatRedisEventType.ERROR) {
            chatSseService.send(
                    event.userMessageId(),
                    "error",
                    new ChatSseErrorResponse(
                            event.userMessageId(),
                            event.code(),
                            event.message()
                    )
            );
            chatSseService.complete(event.userMessageId());
            return;
        }

        log.debug(
                "Ignoring unsupported Redis chat event type={} userMessageId={}",
                event.type(),
                event.userMessageId()
        );
    }
}
