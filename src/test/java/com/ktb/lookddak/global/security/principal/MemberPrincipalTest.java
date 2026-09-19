package com.ktb.lookddak.global.security.principal;

import com.ktb.lookddak.domain.member.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class MemberPrincipalTest {

    @Test
    @DisplayName("회원 정보를 Spring Security 인증 사용자로 변환한다")
    void createMemberPrincipal() {
        Member member = mock(Member.class);
        given(member.getId()).willReturn(1L);
        given(member.getEmail()).willReturn("member@lookddak.com");
        given(member.getPasswordHash()).willReturn("encoded-password");

        MemberPrincipal principal = MemberPrincipal.from(member);

        assertThat(principal.getMemberId()).isEqualTo(1L);
        assertThat(principal.getUsername()).isEqualTo("member@lookddak.com");
        assertThat(principal.getPassword()).isEqualTo("encoded-password");
        assertThat(principal.getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .containsExactly("ROLE_USER");
    }
}
