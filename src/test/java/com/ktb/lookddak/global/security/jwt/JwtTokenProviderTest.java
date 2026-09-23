package com.ktb.lookddak.global.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final String TEST_SECRET =
            "dGVzdC1qd3Qtc2VjcmV0LWtleS1tdXN0LWJlLWF0LWxlYXN0LTMyLWJ5dGVz";

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties(
                TEST_SECRET,
                Duration.ofMinutes(30),
                Duration.ofDays(14)
        );
        jwtTokenProvider = new JwtTokenProvider(properties);
    }

    @Test
    @DisplayName("Access Token에 회원 ID와 토큰 종류를 저장한다")
    void createAccessToken() {
        String token = jwtTokenProvider.createAccessToken(1L);

        Claims claims = jwtTokenProvider.parseClaims(token);

        assertThat(claims.getSubject()).isEqualTo("1");
        assertThat(claims.get("tokenType", String.class)).isEqualTo("ACCESS");
        assertThat(claims.getId()).isNotBlank();
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    @DisplayName("Refresh Token에 회원 ID와 토큰 종류를 저장한다")
    void createRefreshToken() {
        String token = jwtTokenProvider.createRefreshToken(1L);

        Claims claims = jwtTokenProvider.parseClaims(token);
        RefreshTokenClaims refreshTokenClaims =
                jwtTokenProvider.getRefreshTokenClaims(token);

        assertThat(claims.getSubject()).isEqualTo("1");
        assertThat(claims.get("tokenType", String.class)).isEqualTo("REFRESH");
        assertThat(claims.getId()).isNotBlank();
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
        assertThat(refreshTokenClaims.getMemberId()).isEqualTo(1L);
        assertThat(refreshTokenClaims.getTokenId()).isEqualTo(claims.getId());
        assertThat(refreshTokenClaims.getExpiresAt())
                .isEqualTo(claims.getExpiration().toInstant());
    }

    @Test
    @DisplayName("서명이 변조된 토큰은 해석할 수 없다")
    void rejectTamperedToken() {
        String token = jwtTokenProvider.createAccessToken(1L);
        String[] tokenParts = token.split("\\.");
        String signature = tokenParts[2];
        String replacement = signature.startsWith("a") ? "b" : "a";
        String tamperedToken = tokenParts[0]
                + "."
                + tokenParts[1]
                + "."
                + replacement
                + signature.substring(1);

        assertThatThrownBy(() -> jwtTokenProvider.parseClaims(tamperedToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Refresh Token을 Access Token으로 사용할 수 없다")
    void rejectRefreshTokenAsAccessToken() {
        String refreshToken = jwtTokenProvider.createRefreshToken(1L);

        assertThatThrownBy(() ->
                jwtTokenProvider.getMemberIdFromAccessToken(refreshToken)
        ).isInstanceOf(JwtException.class);
    }
}
