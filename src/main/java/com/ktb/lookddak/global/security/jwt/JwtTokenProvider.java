package com.ktb.lookddak.global.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private static final String TOKEN_TYPE_CLAIM = "tokenType";

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(properties.getSecret())
        );
    }

    public String createAccessToken(Long memberId) {
        return createToken(
                memberId,
                TokenType.ACCESS,
                properties.getAccessTokenExpiration()
        );
    }

    public String createRefreshToken(Long memberId) {
        return createToken(
                memberId,
                TokenType.REFRESH,
                properties.getRefreshTokenExpiration()
        );
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getMemberIdFromAccessToken(String token) {
        Claims claims = parseClaims(token);
        validateTokenType(claims, TokenType.ACCESS);

        return Long.valueOf(claims.getSubject());
    }

    public RefreshTokenClaims getRefreshTokenClaims(String token) {
        Claims claims = parseClaims(token);
        validateTokenType(claims, TokenType.REFRESH);

        return new RefreshTokenClaims(
                Long.valueOf(claims.getSubject()),
                claims.getId(),
                claims.getExpiration().toInstant()
        );
    }

    private void validateTokenType(Claims claims, TokenType expectedType) {
        String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);

        if (!expectedType.name().equals(tokenType)) {
            throw new JwtException(expectedType + " Token이 아닙니다.");
        }
    }

    private String createToken(
            Long memberId,
            TokenType tokenType,
            Duration expiration
    ) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(expiration);

        return Jwts.builder()
                .subject(memberId.toString())
                .claim(TOKEN_TYPE_CLAIM, tokenType.name())
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }
}
