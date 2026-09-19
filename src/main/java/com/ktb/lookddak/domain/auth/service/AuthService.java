package com.ktb.lookddak.domain.auth.service;

import com.ktb.lookddak.domain.auth.dto.LoginRequest;
import com.ktb.lookddak.domain.auth.dto.LoginResult;
import com.ktb.lookddak.domain.auth.dto.LoginTokens;
import com.ktb.lookddak.domain.auth.dto.SignUpRequest;
import com.ktb.lookddak.domain.auth.dto.SignUpResponse;
import com.ktb.lookddak.domain.auth.entity.RefreshToken;
import com.ktb.lookddak.domain.auth.repository.RefreshTokenRepository;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.member.repository.MemberRepository;
import com.ktb.lookddak.domain.member.repository.MemberProfileRepository;
import com.ktb.lookddak.global.exception.BusinessException;
import com.ktb.lookddak.global.exception.ErrorCode;
import com.ktb.lookddak.global.security.jwt.JwtTokenProvider;
import com.ktb.lookddak.global.security.jwt.RefreshTokenClaims;
import com.ktb.lookddak.global.security.principal.MemberPrincipal;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberProfileRepository memberProfileRepository;

    @Transactional
    public SignUpResponse signUp(SignUpRequest request) {
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());
        Member member = Member.create(request.getEmail(), passwordHash);
        Member savedMember = memberRepository.save(member);

        return new SignUpResponse(savedMember.getId());
    }

    @Transactional
    public LoginResult login(LoginRequest request) {
        try {
            // 이메일과 비밀번호를 검증하고 인증된 회원 정보를 가져온다.
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
            MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();

            String accessToken = jwtTokenProvider.createAccessToken(principal.getMemberId());
            String refreshToken = jwtTokenProvider.createRefreshToken(principal.getMemberId());

            RefreshTokenClaims refreshTokenClaims =
                    jwtTokenProvider.getRefreshTokenClaims(refreshToken);

            Member member = memberRepository.getReferenceById(principal.getMemberId());

            // JWT 원문 대신 Refresh Token의 식별자(jti)와 만료시간만 저장한다.
            refreshTokenRepository.save(RefreshToken.create(
                    member,
                    refreshTokenClaims.getTokenId(),
                    refreshTokenClaims.getExpiresAt()
            ));

            boolean profileCompleted = memberProfileRepository
                    .existsByMemberId(principal.getMemberId());

            return new LoginResult(
                    new LoginTokens(accessToken, refreshToken),
                    profileCompleted
            );
        } catch (AuthenticationException exception) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
    }

    @Transactional
    public LoginTokens refresh(String refreshTokenValue) {
        // JWT 검증 후 jti를 이용해 서버에 저장된 Refresh Token을 확인한다.
        RefreshTokenClaims claims = parseRefreshToken(refreshTokenValue);
        RefreshToken storedRefreshToken = refreshTokenRepository
                .findByTokenId(claims.getTokenId())
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN)
                );

        Instant now = Instant.now();
        if (!storedRefreshToken.isAvailable(now)
                || !storedRefreshToken.getMember().getId().equals(claims.getMemberId())
                || storedRefreshToken.getMember().getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // Rotation을 위해 기존 Refresh Token을 폐기하고 새로운 토큰을 발급한다.
        storedRefreshToken.revoke(now);

        Long memberId = claims.getMemberId();
        String newAccessToken = jwtTokenProvider.createAccessToken(memberId);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(memberId);

        RefreshTokenClaims newClaims =
                jwtTokenProvider.getRefreshTokenClaims(newRefreshToken);

        refreshTokenRepository.save(RefreshToken.create(
                storedRefreshToken.getMember(),
                newClaims.getTokenId(),
                newClaims.getExpiresAt()
        ));

        return new LoginTokens(newAccessToken, newRefreshToken);
    }

    private RefreshTokenClaims parseRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        try {
            return jwtTokenProvider.getRefreshTokenClaims(refreshToken);
        } catch (JwtException | IllegalArgumentException exception) {
            // JWT 라이브러리 예외를 서비스 공통 예외로 변환한다.
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            return;
        }

        try {
            RefreshTokenClaims claims =
                    jwtTokenProvider.getRefreshTokenClaims(refreshTokenValue);

            refreshTokenRepository.findByTokenId(claims.getTokenId())
                    .filter(refreshToken ->
                            refreshToken.getMember().getId().equals(claims.getMemberId())
                    )
                    .ifPresent(refreshToken -> refreshToken.revoke(Instant.now()));
        } catch (JwtException | IllegalArgumentException exception) {
            // 토큰이 유효하지 않아도 Controller에서 인증 쿠키는 삭제한다.
        }
    }
}
