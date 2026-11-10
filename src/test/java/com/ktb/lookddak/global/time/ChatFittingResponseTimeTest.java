package com.ktb.lookddak.global.time;

import com.ktb.lookddak.domain.chat.dto.ChatMessageCreateResponse;
import com.ktb.lookddak.domain.chat.dto.ChatMessageDetailResponse;
import com.ktb.lookddak.domain.chat.dto.ChatRoomCreateResponse;
import com.ktb.lookddak.domain.chat.dto.ChatRoomListItemResponse;
import com.ktb.lookddak.domain.chat.entity.ChatMessage;
import com.ktb.lookddak.domain.chat.entity.ChatRoom;
import com.ktb.lookddak.domain.fitting.dto.FittingJobCurrentResponse;
import com.ktb.lookddak.domain.fitting.dto.FittingJobStatusResponse;
import com.ktb.lookddak.domain.fitting.dto.FittingResultListItemResponse;
import com.ktb.lookddak.domain.fitting.dto.FittingTempResultDetailResponse;
import com.ktb.lookddak.domain.fitting.entity.FittingJob;
import com.ktb.lookddak.domain.fitting.entity.FittingResult;
import com.ktb.lookddak.domain.fitting.entity.FittingTempResult;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatFittingResponseTimeTest {
    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void serializeAllChatAndFittingTimesWithUtcOffsetAndMicroseconds() throws Exception {
        LocalDateTime stored = LocalDateTime.parse("2026-11-10T00:37:21.096297");
        for (var entry : responses(stored).entrySet()) {
            var json = mapper.readTree(mapper.writeValueAsString(entry.getKey()));
            String value = json.get(entry.getValue()).asString();
            assertThat(value).isEqualTo("2026-11-10T00:37:21.096297Z");
            assertThat(Instant.parse(value).atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime())
                    .isEqualTo(LocalDateTime.parse("2026-11-10T09:37:21.096297"));
        }
    }

    @Test
    void preserveNullTimesWithoutInventingAnInstant() throws Exception {
        for (var entry : responses(null).entrySet()) {
            var json = mapper.readTree(mapper.writeValueAsString(entry.getKey()));
            assertThat(json.get(entry.getValue()).isNull()).isTrue();
        }
    }

    private Map<Object, String> responses(LocalDateTime stored) {
        ChatRoom room = mock(ChatRoom.class);
        when(room.getLastMessageAt()).thenReturn(stored);
        ChatMessage message = mock(ChatMessage.class);
        when(message.getCreatedAt()).thenReturn(stored);
        FittingJob job = mock(FittingJob.class);
        when(job.getCreatedAt()).thenReturn(stored);
        FittingResult result = mock(FittingResult.class);
        when(result.getCreatedAt()).thenReturn(stored);
        FittingTempResult temporary = mock(FittingTempResult.class);
        when(temporary.getExpiresAt()).thenReturn(stored);
        return Map.of(
                new ChatRoomCreateResponse(1L, 2L, stored), "createdAt",
                new ChatMessageCreateResponse(1L, 2L, "안녕하세요", stored), "createdAt",
                ChatMessageDetailResponse.from(message, null), "createdAt",
                ChatRoomListItemResponse.from(room), "lastMessageAt",
                FittingJobStatusResponse.from(job, null), "createdAt",
                FittingJobCurrentResponse.from(job, List.of()), "createdAt",
                FittingResultListItemResponse.from(result, "image"), "savedAt",
                FittingTempResultDetailResponse.from(job, temporary, null, "image", List.of()), "expiresAt"
        );
    }
}
