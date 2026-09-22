package com.ktb.lookddak.domain.recommendation.entity;

import com.ktb.lookddak.domain.chat.entity.ChatMessage;
import com.ktb.lookddak.domain.member.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RecommendationTest {

    @Test
    @DisplayName("회원과 AI 메시지에 연결된 추천을 생성한다")
    void createRecommendation() {
        Member member = Member.create("member@lookddak.com", "encoded");
        ChatMessage message = mock(ChatMessage.class);

        Recommendation recommendation = Recommendation.create(member, message);

        assertThat(recommendation.getMember()).isSameAs(member);
        assertThat(recommendation.getMessage()).isSameAs(message);
    }
}
