package com.ktb.lookddak.domain.chat.redis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatRedisEventTest {

    @Test
    @DisplayName("token 이벤트는 메시지별 순서 번호와 새 토큰만 보관한다")
    void createTokenEvent() {
        ChatRedisEvent event = ChatRedisEvent.token(4365L, 7L, "추천");

        assertThat(event.userMessageId()).isEqualTo(4365L);
        assertThat(event.seq()).isEqualTo(7L);
        assertThat(event.type()).isEqualTo(ChatRedisEventType.TOKEN);
        assertThat(event.content()).isEqualTo("추천");
        assertThat(event.products()).isNull();
    }

    @Test
    @DisplayName("error 이벤트는 본문 대신 오류 code와 message를 보관한다")
    void createErrorEvent() {
        ChatRedisEvent event = ChatRedisEvent.error(
                4365L,
                8L,
                "AI_UNEXPECTED_ERROR",
                "AI 응답 생성 중 오류가 발생했습니다."
        );

        assertThat(event.type()).isEqualTo(ChatRedisEventType.ERROR);
        assertThat(event.content()).isNull();
        assertThat(event.code()).isEqualTo("AI_UNEXPECTED_ERROR");
    }
}
