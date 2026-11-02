package com.ktb.lookddak.domain.chat.redis;

import com.ktb.lookddak.domain.chat.entity.ChatGenerationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings({"rawtypes", "unchecked"})
class ChatRedisSnapshotRepositoryTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ChatRedisSnapshotRepository repository = new ChatRedisSnapshotRepository(
            redisTemplate,
            new ChatRedisKeyFactory(new ChatRedisProperties(false, "lookddak:chat"))
    );

    @Test
    @DisplayName("토큰을 누적할 때 Lua script의 반환 seq를 사용한다")
    void appendTokenReturnsRedisSequence() {
        when(redisTemplate.execute(
                any(DefaultRedisScript.class),
                anyList(),
                anyString(),
                anyString()
        )).thenReturn(7L);

        long seq = repository.appendToken(4365L, "추천");

        assertThat(seq).isEqualTo(7L);
    }

    @Test
    @DisplayName("원자적으로 읽은 content, seq, status를 Snapshot으로 변환한다")
    void findSnapshot() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList()))
                .thenReturn(List.of("안녕하세요", "7", "GENERATING"));

        ChatRedisSnapshot snapshot = repository.findByMessageId(4365L).orElseThrow();

        assertThat(snapshot.content()).isEqualTo("안녕하세요");
        assertThat(snapshot.seq()).isEqualTo(7L);
        assertThat(snapshot.status()).isEqualTo(ChatGenerationStatus.GENERATING);
    }

    @Test
    @DisplayName("Redis에 Snapshot이 없으면 빈 값으로 처리한다")
    void findMissingSnapshot() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList()))
                .thenReturn(List.of("", "", ""));

        assertThat(repository.findByMessageId(4365L)).isEmpty();
    }

    @Test
    @DisplayName("완료나 실패가 아닌 상태 전이는 거절한다")
    void rejectNonTerminalStatus() {
        assertThatThrownBy(() -> repository.transitionStatus(4365L, ChatGenerationStatus.GENERATING))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("terminal");
    }

    @Test
    @DisplayName("Lua script는 token append, seq 증가, 상태 갱신을 한 번에 수행한다")
    void scriptsHaveAtomicOperations() {
        assertThat(ChatRedisSnapshotRepository.appendTokenScript())
                .contains("HINCRBY")
                .contains("APPEND")
                .contains("HSET");
        assertThat(ChatRedisSnapshotRepository.readScript())
                .contains("GET")
                .contains("HGET");
    }
}
