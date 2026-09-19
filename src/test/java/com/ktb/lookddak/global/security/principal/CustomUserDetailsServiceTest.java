package com.ktb.lookddak.global.security.principal;

import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private MemberRepository memberRepository;

    private CustomUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        userDetailsService = new CustomUserDetailsService(memberRepository);
    }

    @Test
    @DisplayName("이메일로 활성 회원의 인증 정보를 조회한다")
    void loadUserByUsername() {
        Member member = createMemberMock();
        given(memberRepository.findByEmailAndDeletedAtIsNull("member@lookddak.com"))
                .willReturn(Optional.of(member));

        MemberPrincipal principal = (MemberPrincipal) userDetailsService
                .loadUserByUsername("member@lookddak.com");

        assertThat(principal.getMemberId()).isEqualTo(1L);
        assertThat(principal.getUsername()).isEqualTo("member@lookddak.com");
    }

    @Test
    @DisplayName("회원 ID로 활성 회원의 인증 정보를 조회한다")
    void loadUserById() {
        Member member = createMemberMock();
        given(memberRepository.findByIdAndDeletedAtIsNull(1L))
                .willReturn(Optional.of(member));

        MemberPrincipal principal = userDetailsService.loadUserById(1L);

        assertThat(principal.getMemberId()).isEqualTo(1L);
        assertThat(principal.getUsername()).isEqualTo("member@lookddak.com");
    }

    @Test
    @DisplayName("활성 회원이 없으면 UsernameNotFoundException이 발생한다")
    void rejectMissingMember() {
        given(memberRepository.findByEmailAndDeletedAtIsNull("missing@lookddak.com"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService
                .loadUserByUsername("missing@lookddak.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("회원을 찾을 수 없습니다.");
    }

    private Member createMemberMock() {
        Member member = mock(Member.class);
        given(member.getId()).willReturn(1L);
        given(member.getEmail()).willReturn("member@lookddak.com");
        given(member.getPasswordHash()).willReturn("encoded-password");
        return member;
    }
}
