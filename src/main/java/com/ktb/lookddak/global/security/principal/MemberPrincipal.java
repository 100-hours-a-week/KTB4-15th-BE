package com.ktb.lookddak.global.security.principal;

import com.ktb.lookddak.domain.member.entity.Member;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class MemberPrincipal implements UserDetails {

    private static final GrantedAuthority USER_AUTHORITY =
            new SimpleGrantedAuthority("ROLE_USER");

    private final Long memberId;
    private final String email;
    private final String password;

    private MemberPrincipal(Long memberId, String email, String password) {
        this.memberId = memberId;
        this.email = email;
        this.password = password;
    }

    public static MemberPrincipal from(Member member) {
        return new MemberPrincipal(
                member.getId(),
                member.getEmail(),
                member.getPasswordHash()
        );
    }

    public Long getMemberId() {
        return memberId;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(USER_AUTHORITY);
    }
}
