package com.ktb.lookddak.domain.auth.service;

import com.ktb.lookddak.domain.auth.dto.SignUpRequest;
import com.ktb.lookddak.domain.auth.dto.SignUpResponse;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.member.repository.MemberRepository;
import com.ktb.lookddak.global.exception.BusinessException;
import com.ktb.lookddak.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private MemberRepository memberRepository;

    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(memberRepository, passwordEncoder);
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
}
