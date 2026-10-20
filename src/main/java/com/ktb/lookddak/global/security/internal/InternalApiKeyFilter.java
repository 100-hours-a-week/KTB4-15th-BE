package com.ktb.lookddak.global.security.internal;

import com.ktb.lookddak.global.exception.ErrorCode;
import com.ktb.lookddak.global.security.handler.SecurityErrorResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
@RequiredArgsConstructor
public class InternalApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Internal-Api-Key";
    private static final String INTERNAL_API_PATH_PREFIX = "/api/v1/internal/";

    private final InternalApiKeyProperties properties;
    private final SecurityErrorResponseWriter responseWriter;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(INTERNAL_API_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String configuredKey = properties.getKey();
        String requestedKey = request.getHeader(HEADER_NAME);

        if (!matches(configuredKey, requestedKey)) {
            responseWriter.write(response, ErrorCode.UNAUTHORIZED);
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        "internal-api",
                        null,
                        AuthorityUtils.createAuthorityList("ROLE_INTERNAL")
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private boolean matches(String configuredKey, String requestedKey) {
        if (!StringUtils.hasText(configuredKey) || !StringUtils.hasText(requestedKey)) {
            return false;
        }

        return MessageDigest.isEqual(
                configuredKey.getBytes(StandardCharsets.UTF_8),
                requestedKey.getBytes(StandardCharsets.UTF_8)
        );
    }
}
