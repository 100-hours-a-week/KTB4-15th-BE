package com.ktb.lookddak.domain.member.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MemberProfileTest {

    @Test
    @DisplayName("회원 기본정보와 검증된 이미지 키로 프로필을 생성한다")
    void createMemberProfile() {
        Member member = Member.create("member@lookddak.com", "encoded-password");

        MemberProfile profile = MemberProfile.create(
                member,
                "John Doe",
                29,
                new BigDecimal("175.5"),
                new BigDecimal("70.3"),
                "full-body/validation/1/test.png"
        );

        assertThat(profile.getMember()).isSameAs(member);
        assertThat(profile.getName()).isEqualTo("John Doe");
        assertThat(profile.getAge()).isEqualTo(29);
        assertThat(profile.getHeight()).isEqualByComparingTo("175.5");
        assertThat(profile.getWeight()).isEqualByComparingTo("70.3");
        assertThat(profile.getFullBodyImageKey())
                .isEqualTo("full-body/validation/1/test.png");
    }
}
