package com.ktb.lookddak.domain.member.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemberTest {

    @Test
    @DisplayName("회원을 생성하면 가격 알림은 비활성화되고 탈퇴 일시는 비어 있다")
    void createMember() {
        Member member = Member.create("member@lookddak.com", "encoded-password");

        assertThat(member.getEmail()).isEqualTo("member@lookddak.com");
        assertThat(member.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(member.isPriceAlertEnabled()).isFalse();
        assertThat(member.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("가격 하락 알림 설정을 변경한다")
    void updatePriceAlertEnabled() {
        Member member = Member.create("member@lookddak.com", "encoded-password");

        member.updatePriceAlertEnabled(true);

        assertThat(member.isPriceAlertEnabled()).isTrue();

        member.updatePriceAlertEnabled(false);

        assertThat(member.isPriceAlertEnabled()).isFalse();
    }
}
