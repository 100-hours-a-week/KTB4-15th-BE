package com.ktb.lookddak.global.security.config;

import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.member.repository.MemberRepository;
import com.ktb.lookddak.global.response.ApiResponse;
import com.ktb.lookddak.global.response.SuccessCode;
import com.ktb.lookddak.global.security.principal.MemberPrincipal;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(SecurityIntegrationTest.ProtectedTestController.class)
class SecurityIntegrationTest {

    private static final String FRONTEND_ORIGIN = "http://localhost:5173";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        memberRepository.save(Member.create(
                "security@lookddak.com",
                passwordEncoder.encode("Test1234!")
        ));
    }

    @Test
    @DisplayName("인증 없이 보호된 API에 접근하면 공통 형식의 401을 반환한다")
    void rejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/test/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
    }

    @Test
    @DisplayName("로그인으로 발급된 Access Token을 사용해 보호된 API에 접근한다")
    void authenticateWithIssuedAccessToken() throws Exception {
        List<String> setCookieHeaders = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "security@lookddak.com",
                                  "password": "Test1234!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andReturn()
                .getResponse()
                .getHeaders(HttpHeaders.SET_COOKIE);

        String accessToken = extractCookieValue(setCookieHeaders, "accessToken");

        mockMvc.perform(get("/api/v1/test/protected")
                        .cookie(new Cookie("accessToken", accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data").isNumber());
    }

    @Test
    @DisplayName("허용된 Origin의 Credential CORS 사전 요청을 허용한다")
    void allowConfiguredCorsOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/test/protected")
                        .header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        FRONTEND_ORIGIN
                ))
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS,
                        "true"
                ));
    }

    private String extractCookieValue(List<String> setCookieHeaders, String cookieName) {
        String prefix = cookieName + "=";

        String cookieHeader = setCookieHeaders.stream()
                .filter(header -> header.startsWith(prefix))
                .findFirst()
                .orElseThrow();

        String cookieValue = cookieHeader.substring(
                prefix.length(),
                cookieHeader.indexOf(';')
        );
        assertThat(cookieValue).isNotBlank();
        return cookieValue;
    }

    @RestController
    static class ProtectedTestController {

        @GetMapping("/api/v1/test/protected")
        ApiResponse<Long> protectedApi(
                @AuthenticationPrincipal MemberPrincipal principal
        ) {
            return ApiResponse.success(SuccessCode.OK, principal.getMemberId());
        }
    }
}
