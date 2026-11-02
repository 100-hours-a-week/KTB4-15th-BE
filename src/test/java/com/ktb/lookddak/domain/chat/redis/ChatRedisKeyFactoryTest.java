package com.ktb.lookddak.domain.chat.redis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatRedisKeyFactoryTest {

    private final ChatRedisKeyFactory keyFactory = new ChatRedisKeyFactory(
            new ChatRedisProperties(false, "lookddak:chat")
    );

    @Test
    @DisplayName("같은 메시지의 content와 state 키는 동일한 Cluster hash tag를 사용한다")
    void useSameHashTagForMessageSnapshotKeys() {
        assertThat(keyFactory.contentKey(4365L))
                .isEqualTo("lookddak:chat:{4365}:content");
        assertThat(keyFactory.stateKey(4365L))
                .isEqualTo("lookddak:chat:{4365}:state");
    }

    @Test
    @DisplayName("메시지별 실시간 이벤트 채널을 생성한다")
    void createEventChannel() {
        assertThat(keyFactory.eventChannel(4365L))
                .isEqualTo("lookddak:chat:events:4365");
    }

    @Test
    @DisplayName("적용 완료 이벤트 키도 메시지와 같은 Cluster hash tag를 사용한다")
    void createAppliedEventKey() {
        assertThat(keyFactory.appliedEventKey(4365L, "event-7"))
                .isEqualTo("lookddak:chat:{4365}:event:event-7");
    }
}
