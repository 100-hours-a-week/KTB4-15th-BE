package com.ktb.lookddak.domain.chat.redis;

import com.ktb.lookddak.domain.chat.entity.ChatGenerationStatus;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Redis에 저장되는 진행 중 AI 응답의 누적 본문과 순번을 관리한다.
 *
 * <p>토큰 추가와 seq 증가는 한 Lua script로 처리한다. 다중 BE 인스턴스가
 * 같은 메시지를 다루더라도 seq가 중복되거나 content와 seq가 따로 갱신되지 않는다.</p>
 *
 * <p>TTL은 아직 여기서 설정하지 않는다. 최종 보관 시간은 재연결/새로고침 요구사항을
 * 측정한 뒤 운영 설정으로 정할 예정이므로, 지금 단계에서 임의의 만료 시간을 강제하지 않는다.</p>
 */
@Repository
public class ChatRedisSnapshotRepository {

    private static final DefaultRedisScript<Long> INITIALIZE_SCRIPT = script(
            """
                    redis.call('SETNX', KEYS[1], '')
                    redis.call('HSETNX', KEYS[2], 'seq', '0')
                    redis.call('HSETNX', KEYS[2], 'status', ARGV[1])
                    return 1
                    """,
            Long.class
    );

    private static final DefaultRedisScript<Long> APPEND_TOKEN_SCRIPT = script(
            """
                    local nextSeq = redis.call('HINCRBY', KEYS[2], 'seq', 1)
                    redis.call('APPEND', KEYS[1], ARGV[1])
                    redis.call('HSET', KEYS[2], 'status', ARGV[2])
                    return nextSeq
                    """,
            Long.class
    );

    private static final DefaultRedisScript<Long> TRANSITION_STATUS_SCRIPT = script(
            """
                    local nextSeq = redis.call('HINCRBY', KEYS[2], 'seq', 1)
                    redis.call('SETNX', KEYS[1], '')
                    redis.call('HSET', KEYS[2], 'status', ARGV[1])
                    return nextSeq
                    """,
            Long.class
    );

    private static final DefaultRedisScript<List> READ_SCRIPT = script(
            """
                    local content = redis.call('GET', KEYS[1])
                    local seq = redis.call('HGET', KEYS[2], 'seq')
                    local status = redis.call('HGET', KEYS[2], 'status')
                    return {content or '', seq or '', status or ''}
                    """,
            List.class
    );

    private final StringRedisTemplate redisTemplate;
    private final ChatRedisKeyFactory keyFactory;

    public ChatRedisSnapshotRepository(
            StringRedisTemplate redisTemplate,
            ChatRedisKeyFactory keyFactory
    ) {
        this.redisTemplate = redisTemplate;
        this.keyFactory = keyFactory;
    }

    /**
     * 메시지 생성 직후 호출한다. 이미 존재하는 Snapshot은 초기화하지 않는다.
     */
    public void initialize(Long userMessageId) {
        redisTemplate.execute(
                INITIALIZE_SCRIPT,
                keys(userMessageId),
                ChatGenerationStatus.GENERATING.name()
        );
    }

    /**
     * 토큰을 누적하고, 해당 토큰의 단조 증가 순번을 반환한다.
     */
    public long appendToken(Long userMessageId, String token) {
        Objects.requireNonNull(token, "token must not be null");

        Long seq = redisTemplate.execute(
                APPEND_TOKEN_SCRIPT,
                keys(userMessageId),
                token,
                ChatGenerationStatus.GENERATING.name()
        );
        return requiredSeq(seq, userMessageId);
    }

    /**
     * 완료 또는 실패 이벤트에도 토큰과 같은 순서 체계를 적용한다.
     */
    public long transitionStatus(Long userMessageId, ChatGenerationStatus status) {
        if (status != ChatGenerationStatus.COMPLETED && status != ChatGenerationStatus.FAILED) {
            throw new IllegalArgumentException("Only terminal statuses can be transitioned: " + status);
        }

        Long seq = redisTemplate.execute(
                TRANSITION_STATUS_SCRIPT,
                keys(userMessageId),
                status.name()
        );
        return requiredSeq(seq, userMessageId);
    }

    /**
     * content, seq, status를 동일한 시점의 값으로 읽는다.
     */
    public Optional<ChatRedisSnapshot> findByMessageId(Long userMessageId) {
        List<?> values = redisTemplate.execute(READ_SCRIPT, keys(userMessageId));
        if (values == null || values.size() != 3) {
            return Optional.empty();
        }

        String content = stringValue(values.get(0));
        String seq = stringValue(values.get(1));
        String status = stringValue(values.get(2));
        if (seq.isEmpty() && status.isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(new ChatRedisSnapshot(
                    userMessageId,
                    content,
                    Long.parseLong(seq),
                    ChatGenerationStatus.valueOf(status)
            ));
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Invalid Redis chat snapshot for userMessageId=" + userMessageId, exception);
        }
    }

    static String initializeScript() {
        return INITIALIZE_SCRIPT.getScriptAsString();
    }

    static String appendTokenScript() {
        return APPEND_TOKEN_SCRIPT.getScriptAsString();
    }

    static String transitionStatusScript() {
        return TRANSITION_STATUS_SCRIPT.getScriptAsString();
    }

    static String readScript() {
        return READ_SCRIPT.getScriptAsString();
    }

    private List<String> keys(Long userMessageId) {
        return List.of(keyFactory.contentKey(userMessageId), keyFactory.stateKey(userMessageId));
    }

    private static long requiredSeq(Long seq, Long userMessageId) {
        if (seq == null) {
            throw new IllegalStateException("Redis did not return seq for userMessageId=" + userMessageId);
        }
        return seq;
    }

    private static String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }

    private static <T> DefaultRedisScript<T> script(String source, Class<T> resultType) {
        DefaultRedisScript<T> script = new DefaultRedisScript<>();
        script.setScriptText(source);
        script.setResultType(resultType);
        return script;
    }
}
