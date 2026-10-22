package com.ktb.lookddak.global.security.cookie;

import com.ktb.lookddak.domain.auth.dto.LoginTokens;
import com.ktb.lookddak.global.security.jwt.JwtAuthenticationFilter;
import com.ktb.lookddak.global.security.jwt.JwtProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthCookieManager {

    public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final String BFF_REFRESH_TOKEN_COOKIE_PATH = "/bff/auth";
    private static final String LEGACY_REFRESH_TOKEN_COOKIE_PATH = "/api/v1/auth";

    private final AuthCookieProperties cookieProperties;
    private final JwtProperties jwtProperties;

    public void addAuthCookies(HttpServletResponse response, LoginTokens tokens) {
        ResponseCookie accessTokenCookie = createCookie(
                JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE_NAME,
                tokens.getAccessToken(),
                "/",
                jwtProperties.getAccessTokenExpiration().toSeconds()
        );
        ResponseCookie bffRefreshTokenCookie = createCookie(
                REFRESH_TOKEN_COOKIE_NAME,
                tokens.getRefreshToken(),
                BFF_REFRESH_TOKEN_COOKIE_PATH,
                jwtProperties.getRefreshTokenExpiration().toSeconds()
        );
        ResponseCookie legacyRefreshTokenCookie = createCookie(
                REFRESH_TOKEN_COOKIE_NAME,
                tokens.getRefreshToken(),
                LEGACY_REFRESH_TOKEN_COOKIE_PATH,
                jwtProperties.getRefreshTokenExpiration().toSeconds()
        );

        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, bffRefreshTokenCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, legacyRefreshTokenCookie.toString());
    }

    public String resolveRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    public void clearAuthCookies(HttpServletResponse response) {
        ResponseCookie accessTokenCookie = createCookie(
                JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE_NAME,
                "",
                "/",
                0
        );
        ResponseCookie bffRefreshTokenCookie = createCookie(
                REFRESH_TOKEN_COOKIE_NAME,
                "",
                BFF_REFRESH_TOKEN_COOKIE_PATH,
                0
        );
        ResponseCookie legacyRefreshTokenCookie = createCookie(
                REFRESH_TOKEN_COOKIE_NAME,
                "",
                LEGACY_REFRESH_TOKEN_COOKIE_PATH,
                0
        );

        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, bffRefreshTokenCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, legacyRefreshTokenCookie.toString());
    }

    private ResponseCookie createCookie(
            String name,
            String value,
            String path,
            long maxAge
    ) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .sameSite(cookieProperties.getSameSite())
                .path(path)
                .maxAge(maxAge)
                .build();
    }
}
