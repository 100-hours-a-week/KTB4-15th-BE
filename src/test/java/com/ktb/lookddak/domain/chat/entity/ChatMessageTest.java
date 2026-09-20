package com.ktb.lookddak.domain.chat.entity;

import com.ktb.lookddak.domain.member.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ChatMessageTest {

    @Test
    @DisplayName("사용자 텍스트 메시지는 AI 응답 생성 중 상태로 생성한다")
    void createUserTextMessage() {
        Member member = Member.create("member@lookddak.com", "encoded-password");
        ChatRoom chatRoom = ChatRoom.create(
                member,
                "5만원대 캐주얼 니트 추천해줘",
                ChatSourceType.GENERAL,
                LocalDateTime.now()
        );

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
}
