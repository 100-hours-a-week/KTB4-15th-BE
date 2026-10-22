package com.ktb.lookddak.global.security.internal;

import com.ktb.lookddak.global.exception.ErrorCode;
import com.ktb.lookddak.global.security.handler.SecurityErrorResponseWriter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InternalApiKeyFilterTest {

    private final InternalApiKeyProperties properties = new InternalApiKeyProperties();
    private final SecurityErrorResponseWriter responseWriter = mock(SecurityErrorResponseWriter.class);
    private final InternalApiKeyFilter filter = new InternalApiKeyFilter(properties, responseWriter);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("일치하는 내부 API 키 요청에 ROLE_INTERNAL 인증을 부여한다")
    void authenticateInternalRequest() throws Exception {
        properties.setKey("test-internal-key");
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/v1/internal/products");
        request.addHeader(InternalApiKeyFilter.HEADER_NAME, "test-internal-key");
        FilterChain chain = mock(FilterChain.class);
        ArgumentCaptor<jakarta.servlet.ServletRequest> requestCaptor =
                ArgumentCaptor.forClass(jakarta.servlet.ServletRequest.class);

        org.mockito.Mockito.doAnswer(invocation -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication).isNotNull();
            assertThat(authentication.getAuthorities())
                    .extracting("authority")
                    .containsExactly("ROLE_INTERNAL");
            return null;
        }).when(chain).doFilter(requestCaptor.capture(), org.mockito.ArgumentMatchers.any());

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        verify(chain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("내부 API 키가 없거나 다르면 요청을 차단한다")
    void rejectInvalidKey() throws Exception {
        properties.setKey("test-internal-key");
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/v1/internal/products");
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(responseWriter).write(response, ErrorCode.UNAUTHORIZED);
        verify(chain, never()).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("내부 경로가 아닌 요청은 API 키 검사 대상이 아니다")
    void skipPublicPath() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products/rankings");

        assertThat(filter.shouldNotFilter(request)).isTrue();
    }
}
