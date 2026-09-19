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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import io.jsonwebtoken.JwtException;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private MemberProfileRepository memberProfileRepository;

    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(
                memberRepository,
                passwordEncoder,
                authenticationManager,
                jwtTokenProvider,
                refreshTokenRepository,
                memberProfileRepository
        );
    }

    @Test
    @DisplayName("회원가입 시 비밀번호를 암호화하고 회원을 저장한다")
    void signUp() {
        SignUpRequest request = new SignUpRequest("member@lookddak.com", "Test1234!");
        Member savedMember = Member.create("member@lookddak.com", "saved-password-hash");
        ReflectionTestUtils.setField(savedMember, "id", 1L);

        given(memberRepository.existsByEmail("member@lookddak.com")).willReturn(false);
        given(memberRepository.save(any(Member.class))).willReturn(savedMember);

        SignUpResponse response = authService.signUp(request);

        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());

        Member memberToSave = memberCaptor.getValue();
        assertThat(memberToSave.getEmail()).isEqualTo("member@lookddak.com");
        assertThat(memberToSave.getPasswordHash()).isNotEqualTo(request.getPassword());
        assertThat(passwordEncoder.matches(request.getPassword(), memberToSave.getPasswordHash())).isTrue();
        assertThat(memberToSave.isPriceAlertEnabled()).isFalse();
        assertThat(response.getMemberId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("이미 가입된 이메일이면 회원가입을 거부한다")
    void rejectDuplicatedEmail() {
        SignUpRequest request = new SignUpRequest("member@lookddak.com", "Test1234!");
        given(memberRepository.existsByEmail("member@lookddak.com")).willReturn(true);

        assertThatThrownBy(() -> authService.signUp(request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS)
                );

        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("로그인에 성공하면 Access Token과 Refresh Token을 생성한다")
    void login() {
        LoginRequest request = new LoginRequest("member@lookddak.com", "Test1234!");
        Authentication authentication = mock(Authentication.class);
        MemberPrincipal principal = mock(MemberPrincipal.class);
        Member member = Member.create("member@lookddak.com", "encoded-password");
        Instant expiresAt = Instant.now().plusSeconds(1209600);

        given(authenticationManager.authenticate(any(Authentication.class)))
                .willReturn(authentication);
        given(authentication.getPrincipal()).willReturn(principal);
        given(principal.getMemberId()).willReturn(1L);
        given(jwtTokenProvider.createAccessToken(1L)).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(1L)).willReturn("refresh-token");
        given(jwtTokenProvider.getRefreshTokenClaims("refresh-token"))
                .willReturn(new RefreshTokenClaims(1L, "refresh-token-id", expiresAt));
        given(memberRepository.getReferenceById(1L)).willReturn(member);
        given(memberProfileRepository.existsByMemberId(1L)).willReturn(true);

        LoginResult result = authService.login(request);

        assertThat(result.getTokens().getAccessToken()).isEqualTo("access-token");
        assertThat(result.getTokens().getRefreshToken()).isEqualTo("refresh-token");
        assertThat(result.isProfileCompleted()).isTrue();

        ArgumentCaptor<RefreshToken> refreshTokenCaptor =
                ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());

        RefreshToken savedRefreshToken = refreshTokenCaptor.getValue();
        assertThat(savedRefreshToken.getMember()).isSameAs(member);
        assertThat(savedRefreshToken.getTokenId()).isEqualTo("refresh-token-id");
        assertThat(savedRefreshToken.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(savedRefreshToken.getRevokedAt()).isNull();
    }

    @Test
    @DisplayName("이메일 또는 비밀번호가 올바르지 않으면 로그인을 거부한다")
    void rejectInvalidCredentials() {
        LoginRequest request = new LoginRequest("member@lookddak.com", "wrong-password");
        given(authenticationManager.authenticate(any(Authentication.class)))
                .willThrow(new BadCredentialsException("인증 실패"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_CREDENTIALS)
                );

        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Refresh Token 재발급 시 기존 토큰을 폐기하고 새 토큰을 저장한다")
    void refresh() {
        Member member = Member.create("member@lookddak.com", "encoded-password");
        ReflectionTestUtils.setField(member, "id", 1L);
        Instant oldExpiresAt = Instant.now().plusSeconds(600);
        Instant newExpiresAt = Instant.now().plusSeconds(1209600);
        RefreshToken storedRefreshToken = RefreshToken.create(
                member,
                "old-token-id",
                oldExpiresAt
        );

        given(jwtTokenProvider.getRefreshTokenClaims("old-refresh-token"))
                .willReturn(new RefreshTokenClaims(1L, "old-token-id", oldExpiresAt));
        given(refreshTokenRepository.findByTokenId("old-token-id"))
                .willReturn(Optional.of(storedRefreshToken));
        given(jwtTokenProvider.createAccessToken(1L)).willReturn("new-access-token");
        given(jwtTokenProvider.createRefreshToken(1L)).willReturn("new-refresh-token");
        given(jwtTokenProvider.getRefreshTokenClaims("new-refresh-token"))
                .willReturn(new RefreshTokenClaims(1L, "new-token-id", newExpiresAt));

        LoginTokens tokens = authService.refresh("old-refresh-token");

        assertThat(tokens.getAccessToken()).isEqualTo("new-access-token");
        assertThat(tokens.getRefreshToken()).isEqualTo("new-refresh-token");
        assertThat(storedRefreshToken.getRevokedAt()).isNotNull();

        ArgumentCaptor<RefreshToken> refreshTokenCaptor =
                ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
        assertThat(refreshTokenCaptor.getValue().getTokenId())
                .isEqualTo("new-token-id");
        assertThat(refreshTokenCaptor.getValue().getMember()).isSameAs(member);
    }

    @Test
    @DisplayName("이미 폐기된 Refresh Token은 재사용할 수 없다")
    void rejectRevokedRefreshToken() {
        Member member = Member.create("member@lookddak.com", "encoded-password");
        ReflectionTestUtils.setField(member, "id", 1L);
        Instant expiresAt = Instant.now().plusSeconds(600);
        RefreshToken storedRefreshToken = RefreshToken.create(
                member,
                "revoked-token-id",
                expiresAt
        );
        storedRefreshToken.revoke(Instant.now());

        given(jwtTokenProvider.getRefreshTokenClaims("revoked-refresh-token"))
                .willReturn(new RefreshTokenClaims(1L, "revoked-token-id", expiresAt));
        given(refreshTokenRepository.findByTokenId("revoked-token-id"))
                .willReturn(Optional.of(storedRefreshToken));

        assertThatThrownBy(() -> authService.refresh("revoked-refresh-token"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN)
                );
    }

    @Test
    @DisplayName("Refresh Token 쿠키가 없으면 재발급을 거부한다")
    void rejectMissingRefreshToken() {
        assertThatThrownBy(() -> authService.refresh(null))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN)
                );

        verify(refreshTokenRepository, never()).findByTokenId(any());
    }

    @Test
    @DisplayName("로그아웃하면 DB의 Refresh Token을 폐기한다")
    void logout() {
        Member member = Member.create("member@lookddak.com", "encoded-password");
        ReflectionTestUtils.setField(member, "id", 1L);
        Instant expiresAt = Instant.now().plusSeconds(600);
        RefreshToken storedRefreshToken = RefreshToken.create(
                member,
                "refresh-token-id",
                expiresAt
        );

        given(jwtTokenProvider.getRefreshTokenClaims("refresh-token"))
                .willReturn(new RefreshTokenClaims(1L, "refresh-token-id", expiresAt));
        given(refreshTokenRepository.findByTokenId("refresh-token-id"))
                .willReturn(Optional.of(storedRefreshToken));

        authService.logout("refresh-token");

        assertThat(storedRefreshToken.getRevokedAt()).isNotNull();
    }

    @Test
    @DisplayName("로그아웃 토큰이 유효하지 않아도 오류를 반환하지 않는다")
    void logoutWithInvalidToken() {
        given(jwtTokenProvider.getRefreshTokenClaims("invalid-token"))
                .willThrow(new JwtException("유효하지 않은 토큰"));

        authService.logout("invalid-token");

        verifyNoInteractions(refreshTokenRepository);
    }
}
