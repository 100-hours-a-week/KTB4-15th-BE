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
    private final ChatRedisProperties properties = new ChatRedisProperties(false, "lookddak:chat");
    private final ChatRedisSnapshotRepository repository = new ChatRedisSnapshotRepository(
            redisTemplate,
            new ChatRedisKeyFactory(properties),
            properties
    );

    @Test
    @DisplayName("새 token 이벤트는 Redis가 발급한 seq로 한 번만 반영한다")
    void appendTokenReturnsAppliedSequence() {
        when(redisTemplate.execute(
                any(DefaultRedisScript.class), anyList(), anyString(), anyString(), anyString()
        )).thenReturn(List.of("APPLIED", "7", "GENERATING"));

        ChatRedisSnapshotWriteResult result = repository.appendToken(4365L, "event-7", "추천");

        assertThat(result.seq()).isEqualTo(7L);
        assertThat(result.applied()).isTrue();
        assertThat(result.status()).isEqualTo(ChatGenerationStatus.GENERATING);
    }

    @Test
    @DisplayName("같은 eventId 재시도는 기존 seq를 반환하고 다시 발행하지 않도록 표시한다")
    void returnDuplicateForRetriedEvent() {
        when(redisTemplate.execute(
                any(DefaultRedisScript.class), anyList(), anyString(), anyString(), anyString()
        )).thenReturn(List.of("DUPLICATE", "7", "GENERATING"));

        ChatRedisSnapshotWriteResult result = repository.appendToken(4365L, "event-7", "추천");

        assertThat(result.seq()).isEqualTo(7L);
        assertThat(result.applied()).isFalse();
    }

    @Test
    @DisplayName("초기화되지 않은 Snapshot에 대한 token 추가는 명확히 거절한다")
    void rejectUninitializedSnapshot() {
        when(redisTemplate.execute(
                any(DefaultRedisScript.class), anyList(), anyString(), anyString(), anyString()
        )).thenReturn(List.of("UNINITIALIZED", "", ""));

        assertThatThrownBy(() -> repository.appendToken(4365L, "event-7", "추천"))
                .isInstanceOf(ChatRedisSnapshotWriteRejectedException.class)
                .hasMessageContaining("not initialized");
    }

    @Test
    @DisplayName("종료 상태인 Snapshot에 다른 token을 적용하려 하면 거절한다")
    void rejectInvalidState() {
        when(redisTemplate.execute(
                any(DefaultRedisScript.class), anyList(), anyString(), anyString(), anyString()
        )).thenReturn(List.of("INVALID_STATE", "10", "COMPLETED"));

        assertThatThrownBy(() -> repository.appendToken(4365L, "event-11", "추가 토큰"))
                .isInstanceOf(ChatRedisSnapshotWriteRejectedException.class)
                .hasMessageContaining("COMPLETED");
    }

    @Test
    @DisplayName("원자적으로 읽은 content, seq, status를 Snapshot으로 변환한다")
    void findSnapshot() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList()))
                .thenReturn(List.of("안녕하세요", "7", "GENERATING", "1"));

        ChatRedisSnapshot snapshot = repository.findByMessageId(4365L).orElseThrow();

        assertThat(snapshot.content()).isEqualTo("안녕하세요");
        assertThat(snapshot.seq()).isEqualTo(7L);
        assertThat(snapshot.status()).isEqualTo(ChatGenerationStatus.GENERATING);
    }

    @Test
    @DisplayName("Redis에 초기화된 Snapshot이 없으면 빈 값으로 처리한다")
    void findMissingSnapshot() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList()))
                .thenReturn(List.of("", "", "", ""));

        assertThat(repository.findByMessageId(4365L)).isEmpty();
    }

    @Test
    @DisplayName("완료나 실패가 아닌 상태 전이는 거절한다")
    void rejectNonTerminalStatus() {
        assertThatThrownBy(() -> repository.transitionStatus(
                4365L, "event-8", ChatGenerationStatus.GENERATING
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("terminal");
    }

    @Test
    @DisplayName("Lua script는 초기화와 eventId 중복을 검사한 뒤 token을 원자적으로 반영한다")
    void scriptsHaveIdempotencyAndStateGuard() {
        assertThat(ChatRedisSnapshotRepository.appendTokenScript())
                .contains("initialized")
                .contains("GET', KEYS[3]")
                .contains("HINCRBY")
                .contains("APPEND")
                .contains("PEXPIRE", "ARGV[3]")
                .contains("DUPLICATE")
                .contains("INVALID_STATE");
        assertThat(ChatRedisSnapshotRepository.transitionStatusScript())
                .contains("currentStatus ~= 'GENERATING'")
                .contains("PEXPIRE", "ARGV[3]")
                .contains("DUPLICATE");
        assertThat(ChatRedisSnapshotRepository.initializeScript())
                .contains("'PX', ARGV[1]")
                .contains("PEXPIRE");
    }
}
