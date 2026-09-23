package com.ktb.lookddak.domain.auth.entity;

import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Entity
@Table(
        name = "refresh_token",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_refresh_token_token_id",
                columnNames = "token_id"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "token_id", nullable = false, length = 36)
    private String tokenId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    private RefreshToken(Member member, String tokenId, Instant expiresAt) {
        this.member = member;
        this.tokenId = tokenId;
        this.expiresAt = expiresAt;
    }

    public static RefreshToken create(
            Member member,
            String tokenId,
            Instant expiresAt
    ) {
        return new RefreshToken(member, tokenId, expiresAt);
    }

    public boolean isAvailable(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    public void revoke(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }
}
