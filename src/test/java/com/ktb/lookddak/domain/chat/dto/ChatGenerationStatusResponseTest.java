package com.ktb.lookddak.domain.chat.dto;

import com.ktb.lookddak.domain.chat.entity.ChatGenerationStatus;
import com.ktb.lookddak.domain.chat.entity.ChatMessage;
import com.ktb.lookddak.domain.chat.entity.ChatRoom;
import com.ktb.lookddak.domain.chat.entity.ChatSourceType;
import com.ktb.lookddak.domain.member.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ChatGenerationStatusResponseTest {

    @Test
    @DisplayName("생성 중이거나 실패한 경우 AI 메시지 없이 상태를 반환한다")
    void createResponseWithoutAiMessage() {
        ChatGenerationStatusResponse generatingResponse =
                new ChatGenerationStatusResponse(
                        ChatGenerationStatus.GENERATING,
                        null
                );
        ChatGenerationStatusResponse failedResponse =
                new ChatGenerationStatusResponse(
                        ChatGenerationStatus.FAILED,
                        null
                );

        assertThat(generatingResponse.getGenerationStatus())
                .isEqualTo(ChatGenerationStatus.GENERATING);
        assertThat(generatingResponse.getMessage()).isNull();
        assertThat(failedResponse.getGenerationStatus())
                .isEqualTo(ChatGenerationStatus.FAILED);
        assertThat(failedResponse.getMessage()).isNull();
    }

    @Test
    @DisplayName("생성 완료된 경우 최종 AI 메시지와 상태를 반환한다")
    void createCompletedResponseWithAiMessage() {
        ChatMessageDetailResponse messageResponse = createAiMessageResponse();

        ChatGenerationStatusResponse response =
                new ChatGenerationStatusResponse(
                        ChatGenerationStatus.COMPLETED,
                        messageResponse
                );

        assertThat(response.getGenerationStatus())
                .isEqualTo(ChatGenerationStatus.COMPLETED);
        assertThat(response.getMessage()).isSameAs(messageResponse);
    }

    @Test
    @DisplayName("생성 상태 응답을 generationStatus와 message 순서로 직렬화한다")
    void serializeResponseInSpecifiedOrder() throws Exception {
        ChatGenerationStatusResponse response =
                new ChatGenerationStatusResponse(
                        ChatGenerationStatus.COMPLETED,
                        createAiMessageResponse()
                );
        ObjectMapper objectMapper = JsonMapper.builder()
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .build();

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).containsSubsequence(
                "\"generationStatus\"",
                "\"message\""
        );
    }

    private ChatMessageDetailResponse createAiMessageResponse() {
        Member member = Member.create(
                "member@lookddak.com",
                "encoded-password"
        );
        ChatRoom chatRoom = ChatRoom.create(
                member,
                "가을 출근용 니트 추천",
                ChatSourceType.GENERAL,
                LocalDateTime.now()
        );
        ChatMessage aiMessage = ChatMessage.createAiText(
                chatRoom,
                "출근할 때 입기 좋은 니트를 추천해드릴게요."
        );
        ReflectionTestUtils.setField(aiMessage, "id", 102L);
        ReflectionTestUtils.setField(
                aiMessage,
                "createdAt",
                LocalDateTime.of(2026, 9, 8, 12, 30, 3)
        );

        return ChatMessageDetailResponse.from(aiMessage, null);
    }
}
