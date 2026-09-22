package com.ktb.lookddak.domain.chat.entity;

import com.ktb.lookddak.domain.member.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatMessageTest {

    @Test
    @DisplayName("사용자 텍스트 메시지는 AI 응답 생성 중 상태로 생성한다")
    void createUserTextMessage() {
        ChatRoom chatRoom = createChatRoom();

        ChatMessage message = ChatMessage.createUserText(
                chatRoom,
                "5만원대 캐주얼 니트 추천해줘"
        );

        assertThat(message.getChatRoom()).isSameAs(chatRoom);
        assertThat(message.getSenderType()).isEqualTo(ChatSenderType.USER);
        assertThat(message.getMessageType()).isEqualTo(ChatMessageType.TEXT);
        assertThat(message.getGenerationStatus())
                .isEqualTo(ChatGenerationStatus.GENERATING);
        assertThat(message.getContent()).isEqualTo("5만원대 캐주얼 니트 추천해줘");
    }

    @Test
    @DisplayName("AI 일반 메시지는 생성 상태 없이 텍스트 유형으로 생성한다")
    void createAiTextMessage() {
        ChatRoom chatRoom = createChatRoom();

        ChatMessage message = ChatMessage.createAiText(
                chatRoom,
                "원하는 색상이 있으신가요?"
        );

        assertThat(message.getSenderType()).isEqualTo(ChatSenderType.AI);
        assertThat(message.getMessageType()).isEqualTo(ChatMessageType.TEXT);
        assertThat(message.getGenerationStatus()).isNull();
        assertThat(message.getContent()).isEqualTo("원하는 색상이 있으신가요?");
    }

    @Test
    @DisplayName("AI 추천 메시지는 생성 상태 없이 추천 유형으로 생성한다")
    void createAiRecommendationMessage() {
        ChatRoom chatRoom = createChatRoom();

        ChatMessage message = ChatMessage.createAiRecommendation(
                chatRoom,
                "조건에 맞는 상품을 추천해드릴게요."
        );

        assertThat(message.getSenderType()).isEqualTo(ChatSenderType.AI);
        assertThat(message.getMessageType())
                .isEqualTo(ChatMessageType.RECOMMENDATION);
        assertThat(message.getGenerationStatus()).isNull();
    }

    @Test
    @DisplayName("생성 중인 사용자 메시지를 완료 상태로 변경한다")
    void completeUserMessageGeneration() {
        ChatMessage message = ChatMessage.createUserText(
                createChatRoom(),
                "니트를 추천해줘"
        );

        message.completeGeneration();

        assertThat(message.getGenerationStatus())
                .isEqualTo(ChatGenerationStatus.COMPLETED);
    }

    @Test
    @DisplayName("생성 중인 사용자 메시지를 실패 상태로 변경한다")
    void failUserMessageGeneration() {
        ChatMessage message = ChatMessage.createUserText(
                createChatRoom(),
                "니트를 추천해줘"
        );

        message.failGeneration();

        assertThat(message.getGenerationStatus())
                .isEqualTo(ChatGenerationStatus.FAILED);
    }

    @Test
    @DisplayName("AI 메시지의 생성 상태는 변경할 수 없다")
    void rejectAiMessageGenerationStatusChange() {
        ChatMessage message = ChatMessage.createAiText(
                createChatRoom(),
                "AI 응답"
        );

        assertThatThrownBy(message::completeGeneration)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("완료된 사용자 메시지의 생성 상태는 다시 변경할 수 없다")
    void rejectCompletedMessageGenerationStatusChange() {
        ChatMessage message = ChatMessage.createUserText(
                createChatRoom(),
                "니트를 추천해줘"
        );
        message.completeGeneration();

        assertThatThrownBy(message::failGeneration)
                .isInstanceOf(IllegalStateException.class);
    }

    private ChatRoom createChatRoom() {
        Member member = Member.create(
                "member@lookddak.com",
                "encoded-password"
        );

        return ChatRoom.create(
                member,
                "5만원대 캐주얼 니트 추천해줘",
                ChatSourceType.GENERAL,
                LocalDateTime.now()
        );
    }
}
