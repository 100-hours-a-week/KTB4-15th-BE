package com.ktb.lookddak.domain.chat.controller;

import com.ktb.lookddak.domain.chat.dto.ChatMessageCreateResponse;
import com.ktb.lookddak.domain.chat.dto.ChatRoomCreateResponse;
import com.ktb.lookddak.domain.chat.service.ChatV2Service;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ChatV2ControllerTest {

    @Mock
    private ChatV2Service chatV2Service;

    @Mock
    private MemberPrincipal principal;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ChatV2Controller(chatV2Service))
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
    @DisplayName("V2 새 대화 생성 요청은 대기 메시지 식별자를 반환한다")
    void createChatRoom() throws Exception {
        given(chatV2Service.createChatRoom(eq(1L), any()))
                .willReturn(new ChatRoomCreateResponse(
                        10L,
                        100L,
                        LocalDateTime.of(2026, 10, 6, 22, 0)
                ));

        mockMvc.perform(post("/api/v2/chat-rooms")
                        .contentType("application/json")
                        .content("""
                                {
                                  "content": "가을 니트를 추천해줘",
                                  "sourceType": "GENERAL"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CREATED"))
                .andExpect(jsonPath("$.data.chatRoomId").value(10))
                .andExpect(jsonPath("$.data.messageId").value(100));

        verify(chatV2Service).createChatRoom(eq(1L), any());
    }

    @Test
    @DisplayName("V2 메시지 전송 요청은 대기 메시지 식별자를 반환한다")
    void createMessage() throws Exception {
        given(chatV2Service.createMessage(eq(1L), eq(10L), any()))
                .willReturn(new ChatMessageCreateResponse(
                        10L,
                        101L,
                        "검은색으로 추천해줘",
                        LocalDateTime.of(2026, 10, 6, 22, 5)
                ));

        mockMvc.perform(post("/api/v2/chat-rooms/10/messages")
                        .contentType("application/json")
                        .content("""
                                {"content": "검은색으로 추천해줘"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CREATED"))
                .andExpect(jsonPath("$.data.chatRoomId").value(10))
                .andExpect(jsonPath("$.data.messageId").value(101));

        verify(chatV2Service).createMessage(eq(1L), eq(10L), any());
    }
}
