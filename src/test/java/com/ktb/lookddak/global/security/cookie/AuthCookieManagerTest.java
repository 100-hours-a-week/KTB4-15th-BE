package com.ktb.lookddak.global.security.cookie;

import com.ktb.lookddak.domain.auth.dto.LoginTokens;
import com.ktb.lookddak.global.security.jwt.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import jakarta.servlet.http.Cookie;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuthCookieManagerTest {

    private AuthCookieManager authCookieManager;

    @BeforeEach
    void setUp() {
        AuthCookieProperties cookieProperties =
                new AuthCookieProperties(false, "Lax");
        JwtProperties jwtProperties = new JwtProperties(
                "dGVzdC1qd3Qtc2VjcmV0LWtleS1tdXN0LWJlLWF0LWxlYXN0LTMyLWJ5dGVz",
                Duration.ofMinutes(30),
                Duration.ofDays(14)
        );
        authCookieManager = new AuthCookieManager(cookieProperties, jwtProperties);
    }

    @Test
    @DisplayName("Access Token과 Refresh Token을 HttpOnly 쿠키로 추가한다")
    void addLoginCookies() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        LoginTokens tokens = new LoginTokens("access-token", "refresh-token");

        authCookieManager.addAuthCookies(response, tokens);

        List<String> cookies = response.getHeaders(HttpHeaders.SET_COOKIE);
        assertThat(cookies).hasSize(2);
        assertThat(cookies.get(0))
                .contains("accessToken=access-token")
                .contains("Path=/")
                .contains("Max-Age=1800")
                .contains("HttpOnly")
                .contains("SameSite=Lax")
                .doesNotContain("Secure");
        assertThat(cookies.get(1))
                .contains("refreshToken=refresh-token")
                .contains("Path=/api/v1/auth")
                .contains("Max-Age=1209600")
                .contains("HttpOnly")
                .contains("SameSite=Lax")
                .doesNotContain("Secure");
    }

    @Test
    @DisplayName("요청 쿠키에서 Refresh Token을 찾는다")
    void resolveRefreshToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(
                new Cookie("other", "other-value"),
                new Cookie("refreshToken", "refresh-token")
        );

        String refreshToken = authCookieManager.resolveRefreshToken(request);

        assertThat(refreshToken).isEqualTo("refresh-token");
    }

    @Test
    @DisplayName("로그아웃 시 Access Token과 Refresh Token 쿠키를 삭제한다")
    void clearAuthCookies() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        authCookieManager.clearAuthCookies(response);

        List<String> cookies = response.getHeaders(HttpHeaders.SET_COOKIE);
        assertThat(cookies).hasSize(2);
        assertThat(cookies.get(0))
                .contains("accessToken=")
                .contains("Path=/")
                .contains("Max-Age=0")
                .contains("HttpOnly")
                .contains("SameSite=Lax");
        assertThat(cookies.get(1))
                .contains("refreshToken=")
                .contains("Path=/api/v1/auth")
                .contains("Max-Age=0")
                .contains("HttpOnly")
                .contains("SameSite=Lax");
    }
}
