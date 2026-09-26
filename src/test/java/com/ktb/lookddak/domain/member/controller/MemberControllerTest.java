package com.ktb.lookddak.domain.member.controller;

import com.ktb.lookddak.domain.member.dto.MemberMeResponse;
import com.ktb.lookddak.domain.member.service.MemberService;
import com.ktb.lookddak.global.exception.GlobalExceptionHandler;
import com.ktb.lookddak.global.security.principal.MemberPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MemberControllerTest {

    @Mock
    private MemberService memberService;
    @Mock
    private MemberPrincipal principal;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MemberController controller = new MemberController(memberService);
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(
                        new AuthenticationPrincipalArgumentResolver()
                )
                .build();

        lenient().when(principal.getMemberId()).thenReturn(1L);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        null,
                        List.of()
                )
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("인증된 회원의 프로필 등록 여부를 반환한다")
    void getMe() throws Exception {
        given(memberService.getMe(1L))
                .willReturn(new MemberMeResponse(true));

        mockMvc.perform(get("/api/v1/members/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.profileCompleted").value(true))
                .andExpect(jsonPath("$.message")
                        .value("요청이 성공적으로 처리되었습니다."));

        verify(memberService).getMe(1L);
    }
}
