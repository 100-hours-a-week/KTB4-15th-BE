package com.ktb.lookddak.global.security.jwt;

import com.ktb.lookddak.global.security.principal.CustomUserDetailsService;
import com.ktb.lookddak.global.security.principal.MemberPrincipal;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class JwtAuthenticationFilterTest {

    private static final String TEST_SECRET =
            "dGVzdC1qd3Qtc2VjcmV0LWtleS1tdXN0LWJlLWF0LWxlYXN0LTMyLWJ5dGVz";

    private CustomUserDetailsService userDetailsService;
    private JwtTokenProvider jwtTokenProvider;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        userDetailsService = mock(CustomUserDetailsService.class);
        JwtProperties properties = new JwtProperties(
                TEST_SECRET,
                Duration.ofMinutes(30),
                Duration.ofDays(14)
        );
        jwtTokenProvider = new JwtTokenProvider(properties);
        filter = new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("유효한 Access Token으로 인증 정보를 등록한다")
    void authenticateWithAccessToken() throws Exception {
        String accessToken = jwtTokenProvider.createAccessToken(1L);
        MemberPrincipal principal = mock(MemberPrincipal.class);
        given(principal.getAuthorities()).willReturn(List.of());
        given(userDetailsService.loadUserById(1L)).willReturn(principal);
        MockHttpServletRequest request = requestWithAccessToken(accessToken);

        filter.doFilter(
                request,
                new MockHttpServletResponse(),
                new MockFilterChain()
        );

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isSameAs(principal);
        assertThat(SecurityContextHolder.getContext().getAuthentication().isAuthenticated())
                .isTrue();
    }

    @Test
    @DisplayName("Access Token 쿠키가 없으면 인증을 시도하지 않는다")
    void skipRequestWithoutAccessToken() throws Exception {
        filter.doFilter(
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                new MockFilterChain()
        );

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(userDetailsService);
    }

    @Test
    @DisplayName("Refresh Token으로는 인증 정보를 등록하지 않는다")
    void rejectRefreshToken() throws Exception {
        String refreshToken = jwtTokenProvider.createRefreshToken(1L);
        MockHttpServletRequest request = requestWithAccessToken(refreshToken);

        filter.doFilter(
                request,
                new MockHttpServletResponse(),
                new MockFilterChain()
        );

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(userDetailsService);
    }

    private MockHttpServletRequest requestWithAccessToken(String accessToken) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(
                JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE_NAME,
                accessToken
        ));
        return request;
    }
}
