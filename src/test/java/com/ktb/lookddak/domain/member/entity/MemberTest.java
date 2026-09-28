package com.ktb.lookddak.domain.member.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemberTest {

    @Test
    @DisplayName("회원을 생성하면 가격 알림과 가상피팅 요청 횟수가 초기화된다")
    void createMember() {
        Member member = Member.create("member@lookddak.com", "encoded-password");

        assertThat(member.getEmail()).isEqualTo("member@lookddak.com");
        assertThat(member.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(member.isPriceAlertEnabled()).isFalse();
        assertThat(member.getFittingRequestCount()).isZero();
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

    @Test
    @DisplayName("가상피팅 요청 횟수를 증가시키고 제한 도달 여부를 확인한다")
    void increaseFittingRequestCount() {
        Member member = Member.create("member@lookddak.com", "encoded-password");

        for (int count = 0; count < 9; count++) {
            member.increaseFittingRequestCount();
        }

        assertThat(member.getFittingRequestCount()).isEqualTo(9);
        assertThat(member.hasReachedFittingRequestLimit(10)).isFalse();

        member.increaseFittingRequestCount();

        assertThat(member.getFittingRequestCount()).isEqualTo(10);
        assertThat(member.hasReachedFittingRequestLimit(10)).isTrue();
    }
}
