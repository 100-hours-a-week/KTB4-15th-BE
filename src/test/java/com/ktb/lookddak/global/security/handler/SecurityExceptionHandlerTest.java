package com.ktb.lookddak.global.security.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityExceptionHandlerTest {

    private SecurityErrorResponseWriter responseWriter;

    @BeforeEach
    void setUp() {
        responseWriter = new SecurityErrorResponseWriter(new ObjectMapper());
    }

    @Test
    @DisplayName("인증되지 않은 요청에 공통 형식의 401 응답을 반환한다")
    void handleUnauthorized() throws Exception {
        CustomAuthenticationEntryPoint entryPoint =
                new CustomAuthenticationEntryPoint(responseWriter);
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(
                new MockHttpServletRequest(),
                response,
                new BadCredentialsException("인증 실패")
        );

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString()).isEqualTo(
                "{\"code\":\"UNAUTHORIZED\",\"data\":null,"
                        + "\"message\":\"인증이 필요합니다.\"}"
        );
    }

    @Test
    @DisplayName("권한이 부족한 요청에 공통 형식의 403 응답을 반환한다")
    void handleForbidden() throws Exception {
        CustomAccessDeniedHandler deniedHandler =
                new CustomAccessDeniedHandler(responseWriter);
        MockHttpServletResponse response = new MockHttpServletResponse();

        deniedHandler.handle(
                new MockHttpServletRequest(),
                response,
                new AccessDeniedException("권한 부족")
        );

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString()).isEqualTo(
                "{\"code\":\"FORBIDDEN\",\"data\":null,"
                        + "\"message\":\"접근 권한이 없습니다.\"}"
        );
    }
}
