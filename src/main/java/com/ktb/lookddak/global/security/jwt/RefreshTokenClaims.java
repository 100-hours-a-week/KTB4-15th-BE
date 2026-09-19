package com.ktb.lookddak.global.security.jwt;

import lombok.Getter;

import java.time.Instant;

@Getter
public class RefreshTokenClaims {

    private final Long memberId;
    private final String tokenId;
    private final Instant expiresAt;

    public RefreshTokenClaims(
            Long memberId,
            String tokenId,
            Instant expiresAt
    ) {
        this.memberId = memberId;
        this.tokenId = tokenId;
        this.expiresAt = expiresAt;
    }
}
