package com.ktb.lookddak.domain.member.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

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
    @DisplayName("같은 날 가상피팅 이용권을 차감하고 제한 도달 여부를 확인한다")
    void consumeFittingRequestQuota() {
        Member member = Member.create("member@lookddak.com", "encoded-password");
        LocalDate today = LocalDate.of(2026, 10, 21);

        for (int count = 0; count < 24; count++) {
            member.consumeFittingRequestQuota(today);
        }

        assertThat(member.getFittingRequestCount()).isEqualTo(24);
        assertThat(member.hasReachedFittingRequestLimit(today, 25)).isFalse();

        member.consumeFittingRequestQuota(today);

        assertThat(member.getFittingRequestCount()).isEqualTo(25);
        assertThat(member.hasReachedFittingRequestLimit(today, 25)).isTrue();
    }

    @Test
    @DisplayName("날짜가 바뀌면 첫 이용권 차감 시 일일 사용량을 초기화한다")
    void resetQuotaWhenDateChanges() {
        Member member = Member.create("member@lookddak.com", "encoded-password");
        LocalDate yesterday = LocalDate.of(2026, 10, 20);
        LocalDate today = yesterday.plusDays(1);

        member.consumeFittingRequestQuota(yesterday);
        member.consumeFittingRequestQuota(yesterday);
        member.consumeFittingRequestQuota(today);

        assertThat(member.getFittingRequestDate()).isEqualTo(today);
        assertThat(member.getFittingRequestCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("이용권을 차감한 날짜와 현재 기준 날짜가 같을 때만 환불한다")
    void refundFittingRequestQuota() {
        Member member = Member.create("member@lookddak.com", "encoded-password");
        LocalDate chargedDate = LocalDate.of(2026, 10, 21);

        member.consumeFittingRequestQuota(chargedDate);
        member.refundFittingRequestQuota(chargedDate);

        assertThat(member.getFittingRequestCount()).isZero();

        member.consumeFittingRequestQuota(chargedDate.plusDays(1));
        member.refundFittingRequestQuota(chargedDate);

        assertThat(member.getFittingRequestCount()).isEqualTo(1);
    }
}
