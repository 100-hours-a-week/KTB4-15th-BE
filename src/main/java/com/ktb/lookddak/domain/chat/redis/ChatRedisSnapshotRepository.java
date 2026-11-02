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
 * <p>각 변경은 호출자가 전달한 eventId로 식별한다. Redis 응답 유실 뒤 같은 eventId로
 * 재시도하면 기존 seq만 반환하고, content와 seq는 다시 변경하지 않는다.</p>
 *
 * <p>TTL은 아직 여기서 설정하지 않는다. 최종 보관 시간은 재연결/새로고침 요구사항을
 * 측정한 뒤 운영 설정으로 정할 예정이므로, 지금 단계에서 임의의 만료 시간을 강제하지 않는다.</p>
 */
@Repository
public class ChatRedisSnapshotRepository {

    private static final String APPLIED = "APPLIED";
    private static final String DUPLICATE = "DUPLICATE";
    private static final String UNINITIALIZED = "UNINITIALIZED";
    private static final String INVALID_STATE = "INVALID_STATE";

    private static final DefaultRedisScript<Long> INITIALIZE_SCRIPT = script(
            """
                    local initialized = redis.call('HGET', KEYS[2], 'initialized')
                    if initialized == '1' then
                        return 0
                    end
                    redis.call('SET', KEYS[1], '')
                    redis.call('HSET', KEYS[2],
                        'initialized', '1',
                        'seq', '0',
                        'status', 'GENERATING')
                    return 1
                    """,
            Long.class
    );

    private static final DefaultRedisScript<List> APPEND_TOKEN_SCRIPT = script(
            """
                    local initialized = redis.call('HGET', KEYS[2], 'initialized')
                    if initialized ~= '1' then
                        return {'UNINITIALIZED', '', ''}
                    end

                    local currentStatus = redis.call('HGET', KEYS[2], 'status')
                    local existingSeq = redis.call('GET', KEYS[3])
                    if existingSeq then
                        return {'DUPLICATE', existingSeq, currentStatus}
                    end
                    if currentStatus ~= 'GENERATING' then
                        return {'INVALID_STATE', redis.call('HGET', KEYS[2], 'seq'), currentStatus}
                    end

                    local nextSeq = redis.call('HINCRBY', KEYS[2], 'seq', 1)
                    redis.call('APPEND', KEYS[1], ARGV[1])
                    redis.call('SET', KEYS[3], tostring(nextSeq))
                    redis.call('HSET', KEYS[2], 'lastEventId', ARGV[2])
                    return {'APPLIED', tostring(nextSeq), currentStatus}
                    """,
            List.class
    );

    private static final DefaultRedisScript<List> TRANSITION_STATUS_SCRIPT = script(
            """
                    local initialized = redis.call('HGET', KEYS[2], 'initialized')
                    if initialized ~= '1' then
                        return {'UNINITIALIZED', '', ''}
                    end

                    local currentStatus = redis.call('HGET', KEYS[2], 'status')
                    local existingSeq = redis.call('GET', KEYS[3])
                    if existingSeq then
                        return {'DUPLICATE', existingSeq, currentStatus}
                    end
                    if currentStatus ~= 'GENERATING' then
                        return {'INVALID_STATE', redis.call('HGET', KEYS[2], 'seq'), currentStatus}
                    end

                    local nextSeq = redis.call('HINCRBY', KEYS[2], 'seq', 1)
                    redis.call('SET', KEYS[3], tostring(nextSeq))
                    redis.call('HSET', KEYS[2],
                        'status', ARGV[1],
                        'lastEventId', ARGV[2])
                    return {'APPLIED', tostring(nextSeq), ARGV[1]}
                    """,
            List.class
    );

    private static final DefaultRedisScript<List> READ_SCRIPT = script(
            """
                    local initialized = redis.call('HGET', KEYS[2], 'initialized')
                    if initialized ~= '1' then
                        return {'', '', '', ''}
                    end
                    local content = redis.call('GET', KEYS[1])
                    local seq = redis.call('HGET', KEYS[2], 'seq')
                    local status = redis.call('HGET', KEYS[2], 'status')
                    return {content or '', seq or '', status or '', initialized}
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
     * 메시지 생성 직후 호출한다. 같은 메시지에 대한 중복 초기화는 기존 Snapshot을 보존한다.
     */
    public void initialize(Long userMessageId) {
        redisTemplate.execute(INITIALIZE_SCRIPT, snapshotKeys(userMessageId));
    }

    /**
     * token을 한 번만 누적한다. Redis timeout 뒤에는 반드시 같은 eventId로 재시도해야 한다.
     */
    public ChatRedisSnapshotWriteResult appendToken(
            Long userMessageId,
            String eventId,
            String token
    ) {
        requireEventId(eventId);
        Objects.requireNonNull(token, "token must not be null");

        List<?> result = redisTemplate.execute(
                APPEND_TOKEN_SCRIPT,
                writeKeys(userMessageId, eventId),
                token,
                eventId
        );
        return toWriteResult(result, userMessageId, eventId);
    }

    /**
     * 생성 중인 메시지만 완료 또는 실패로 바꾼다. 같은 terminal eventId의 재시도는 멱등하다.
     */
    public ChatRedisSnapshotWriteResult transitionStatus(
            Long userMessageId,
            String eventId,
            ChatGenerationStatus status
    ) {
        requireEventId(eventId);
        if (status != ChatGenerationStatus.COMPLETED && status != ChatGenerationStatus.FAILED) {
            throw new IllegalArgumentException("Only terminal statuses can be transitioned: " + status);
        }

        List<?> result = redisTemplate.execute(
                TRANSITION_STATUS_SCRIPT,
                writeKeys(userMessageId, eventId),
                status.name(),
                eventId
        );
        return toWriteResult(result, userMessageId, eventId);
    }

    /** content, seq, status를 동일한 시점의 값으로 읽는다. */
    public Optional<ChatRedisSnapshot> findByMessageId(Long userMessageId) {
        List<?> values = redisTemplate.execute(READ_SCRIPT, snapshotKeys(userMessageId));
        if (values == null || values.size() != 4 || stringValue(values.get(3)).isEmpty()) {
            return Optional.empty();
        }

        String content = stringValue(values.get(0));
        String seq = stringValue(values.get(1));
        String status = stringValue(values.get(2));
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

    private List<String> snapshotKeys(Long userMessageId) {
        return List.of(keyFactory.contentKey(userMessageId), keyFactory.stateKey(userMessageId));
    }

    private List<String> writeKeys(Long userMessageId, String eventId) {
        return List.of(
                keyFactory.contentKey(userMessageId),
                keyFactory.stateKey(userMessageId),
                keyFactory.appliedEventKey(userMessageId, eventId)
        );
    }

    private static ChatRedisSnapshotWriteResult toWriteResult(
            List<?> values,
            Long userMessageId,
            String eventId
    ) {
        if (values == null || values.size() != 3) {
            throw new IllegalStateException("Invalid Redis write result for userMessageId=" + userMessageId);
        }

        String outcome = stringValue(values.get(0));
        String seq = stringValue(values.get(1));
        String status = stringValue(values.get(2));
        if (UNINITIALIZED.equals(outcome)) {
            throw new ChatRedisSnapshotWriteRejectedException(
                    "Redis Snapshot is not initialized for userMessageId=" + userMessageId
            );
        }
        if (INVALID_STATE.equals(outcome)) {
            throw new ChatRedisSnapshotWriteRejectedException(
                    "Cannot apply eventId=" + eventId + " to Redis Snapshot status=" + status
            );
        }
        if (!APPLIED.equals(outcome) && !DUPLICATE.equals(outcome)) {
            throw new IllegalStateException("Unknown Redis write result=" + outcome);
        }

        try {
            return new ChatRedisSnapshotWriteResult(
                    Long.parseLong(seq),
                    APPLIED.equals(outcome),
                    ChatGenerationStatus.valueOf(status)
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Invalid Redis write result for userMessageId=" + userMessageId, exception);
        }
    }

    private static void requireEventId(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId must not be blank");
        }
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
