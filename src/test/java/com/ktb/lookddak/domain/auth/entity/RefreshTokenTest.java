package com.ktb.lookddak.domain.auth.entity;

import com.ktb.lookddak.domain.member.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenTest {

    @Test
    @DisplayName("회원과 토큰 식별자 및 만료시간으로 Refresh Token을 생성한다")
    void createRefreshToken() {
        Member member = Member.create("member@lookddak.com", "encoded-password");
        Instant expiresAt = Instant.now().plusSeconds(1209600);

        RefreshToken refreshToken = RefreshToken.create(
                member,
                "550e8400-e29b-41d4-a716-446655440000",
                expiresAt
        );

        assertThat(refreshToken.getMember()).isSameAs(member);
        assertThat(refreshToken.getTokenId())
                .isEqualTo("550e8400-e29b-41d4-a716-446655440000");
        assertThat(refreshToken.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(refreshToken.getRevokedAt()).isNull();
    }

    @Test
    @DisplayName("Refresh Token을 폐기하면 더 이상 사용할 수 없다")
    void revokeRefreshToken() {
        Member member = Member.create("member@lookddak.com", "encoded-password");
        Instant now = Instant.now();
        RefreshToken refreshToken = RefreshToken.create(
                member,
                "550e8400-e29b-41d4-a716-446655440000",
                now.plusSeconds(60)
        );

        assertThat(refreshToken.isAvailable(now)).isTrue();

        refreshToken.revoke(now);

        assertThat(refreshToken.isAvailable(now)).isFalse();
        assertThat(refreshToken.getRevokedAt()).isEqualTo(now);
    }
}
