package com.ktb.lookddak.domain.chat.controller;

import com.ktb.lookddak.domain.chat.dto.ChatMessageCreateResponse;
import com.ktb.lookddak.domain.chat.dto.ChatRoomCreateResponse;
import com.ktb.lookddak.domain.chat.service.ChatService;
import com.ktb.lookddak.global.exception.BusinessException;
import com.ktb.lookddak.global.exception.ErrorCode;
import com.ktb.lookddak.global.exception.GlobalExceptionHandler;
import com.ktb.lookddak.global.security.principal.MemberPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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
class ChatControllerTest {

    @Mock
    private ChatService chatService;

    @Mock
    private MemberPrincipal principal;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ChatController controller = new ChatController(chatService);
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
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
    @DisplayName("새 대화를 시작하면 201 Created를 반환한다")
    void createChatRoom() throws Exception {
        given(chatService.createChatRoom(any(), any()))
                .willReturn(new ChatRoomCreateResponse(10L, 100L));

        mockMvc.perform(post("/api/v1/chat-rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "5만원대 캐주얼 니트 추천해줘",
                                  "sourceType": "GENERAL"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CREATED"))
                .andExpect(jsonPath("$.data.chatRoomId").value(10))
                .andExpect(jsonPath("$.data.messageId").value(100))
                .andExpect(jsonPath("$.message")
                        .value("리소스가 성공적으로 생성되었습니다."));

        verify(chatService).createChatRoom(eq(1L), any());
    }

    @Test
    @DisplayName("기존 채팅방에 메시지를 전송하면 201 Created를 반환한다")
    void createMessage() throws Exception {
        given(chatService.createMessage(any(), any(), any()))
                .willReturn(new ChatMessageCreateResponse(
                        10L,
                        101L,
                        "검은색으로 추천해줘"
                ));

        mockMvc.perform(post("/api/v1/chat-rooms/10/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "검은색으로 추천해줘"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CREATED"))
                .andExpect(jsonPath("$.data.chatRoomId").value(10))
                .andExpect(jsonPath("$.data.messageId").value(101))
                .andExpect(jsonPath("$.data.content").value("검은색으로 추천해줘"));

        verify(chatService).createMessage(eq(1L), eq(10L), any());
    }

    @Test
    @DisplayName("채팅 요청 내용이 공백이면 400 Bad Request를 반환한다")
    void rejectBlankContent() throws Exception {
        mockMvc.perform(post("/api/v1/chat-rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "   ",
                                  "sourceType": "GENERAL"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("지원하지 않는 출처 유형이면 400 Bad Request를 반환한다")
    void rejectInvalidSourceType() throws Exception {
        mockMvc.perform(post("/api/v1/chat-rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "옷을 추천해줘",
                                  "sourceType": "INVALID"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_JSON_FORMAT"));
    }

    @Test
    @DisplayName("채팅방이 없으면 404 Not Found를 반환한다")
    void rejectMissingChatRoom() throws Exception {
        given(chatService.createMessage(any(), any(), any()))
                .willThrow(new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        performCreateMessage()
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CHAT_ROOM_NOT_FOUND"));
    }

    @Test
    @DisplayName("다른 회원의 채팅방이면 403 Forbidden을 반환한다")
    void rejectChatRoomAccess() throws Exception {
        given(chatService.createMessage(any(), any(), any()))
                .willThrow(new BusinessException(ErrorCode.CHAT_ROOM_ACCESS_DENIED));

        performCreateMessage()
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CHAT_ROOM_ACCESS_DENIED"));
    }

    @Test
    @DisplayName("AI 응답 생성 중이면 409 Conflict를 반환한다")
    void rejectWhileGeneratingResponse() throws Exception {
        given(chatService.createMessage(any(), any(), any()))
                .willThrow(new BusinessException(ErrorCode.AI_RESPONSE_GENERATING));

        performCreateMessage()
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AI_RESPONSE_GENERATING"));
    }

    private org.springframework.test.web.servlet.ResultActions performCreateMessage()
            throws Exception {
        return mockMvc.perform(post("/api/v1/chat-rooms/10/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "content": "검은색으로 추천해줘"
                        }
                        """));
    }
}
