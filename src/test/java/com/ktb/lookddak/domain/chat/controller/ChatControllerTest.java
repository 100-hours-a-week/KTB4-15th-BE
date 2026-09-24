package com.ktb.lookddak.domain.chat.controller;

import com.ktb.lookddak.domain.chat.dto.ChatMessageCreateResponse;
import com.ktb.lookddak.domain.chat.dto.ChatMessageDetailResponse;
import com.ktb.lookddak.domain.chat.dto.ChatGenerationStatusResponse;
import com.ktb.lookddak.domain.chat.dto.ChatRoomCreateResponse;
import com.ktb.lookddak.domain.chat.dto.ChatRoomDetailResponse;
import com.ktb.lookddak.domain.chat.dto.ChatRoomListItemResponse;
import com.ktb.lookddak.domain.chat.dto.ChatRoomListResponse;
import com.ktb.lookddak.domain.chat.dto.ChatRoomTitleUpdateResponse;
import com.ktb.lookddak.domain.chat.dto.RecommendationResponse;
import com.ktb.lookddak.domain.chat.dto.RecommendedProductResponse;
import com.ktb.lookddak.domain.chat.entity.ChatMessage;
import com.ktb.lookddak.domain.chat.entity.ChatGenerationStatus;
import com.ktb.lookddak.domain.chat.entity.ChatRoom;
import com.ktb.lookddak.domain.chat.entity.ChatSourceType;
import com.ktb.lookddak.domain.chat.service.ChatService;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.product.entity.Product;
import com.ktb.lookddak.domain.product.entity.ProductItemType;
import com.ktb.lookddak.domain.recommendation.entity.Recommendation;
import com.ktb.lookddak.domain.recommendation.entity.RecommendationProduct;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
    @DisplayName("Query Parameter가 없으면 기본 크기로 첫 채팅방 목록을 반환한다")
    void getFirstChatRoomPage() throws Exception {
        LocalDateTime lastMessageAt = LocalDateTime.of(2026, 9, 21, 15, 0);
        ChatRoomListItemResponse item = ChatRoomListItemResponse.from(
                createChatRoom(25L, lastMessageAt)
        );
        given(chatService.getChatRooms(1L, null, 20))
                .willReturn(new ChatRoomListResponse(
                        List.of(item),
                        25L,
                        true
                ));

        mockMvc.perform(get("/api/v1/chat-rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.items[0].chatRoomId").value(25))
                .andExpect(jsonPath("$.data.items[0].title")
                        .value("가을 출근용 니트 추천"))
                .andExpect(jsonPath("$.data.items[0].lastMessageAt")
                        .value("2026-09-21T15:00:00"))
                .andExpect(jsonPath("$.data.nextCursor").value(25))
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andExpect(jsonPath("$.message")
                        .value("요청이 성공적으로 처리되었습니다."));

        verify(chatService).getChatRooms(1L, null, 20);
    }

    @Test
    @DisplayName("Cursor와 조회 크기를 다음 채팅방 목록 조회에 사용한다")
    void getNextChatRoomPage() throws Exception {
        given(chatService.getChatRooms(1L, 25L, 10))
                .willReturn(new ChatRoomListResponse(List.of(), null, false));

        mockMvc.perform(get("/api/v1/chat-rooms")
                        .queryParam("cursor", "25")
                        .queryParam("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.nextCursor").isEmpty())
                .andExpect(jsonPath("$.data.hasNext").value(false));

        verify(chatService).getChatRooms(1L, 25L, 10);
    }

    @Test
    @DisplayName("잘못된 Pagination 조건은 400 Bad Request를 반환한다")
    void rejectInvalidPaginationParameter() throws Exception {
        given(chatService.getChatRooms(1L, 0L, 20))
                .willThrow(new BusinessException(
                        ErrorCode.INVALID_PAGINATION_PARAMETER
                ));

        mockMvc.perform(get("/api/v1/chat-rooms")
                        .queryParam("cursor", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_PAGINATION_PARAMETER"))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.message")
                        .value("올바른 조회 조건을 입력해주세요."));
    }

    @Test
    @DisplayName("숫자가 아닌 Cursor는 공통 입력값 오류를 반환한다")
    void rejectNonNumericCursor() throws Exception {
        mockMvc.perform(get("/api/v1/chat-rooms")
                        .queryParam("cursor", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("특정 채팅방의 메시지와 추천 상품을 반환한다")
    void getChatRoomDetail() throws Exception {
        ChatRoom chatRoom = createChatRoom(
                123L,
                LocalDateTime.of(2026, 9, 8, 12, 30)
        );
        ChatMessage message = ChatMessage.createAiRecommendation(
                chatRoom,
                "출근할 때 입기 좋은 니트를 추천해드릴게요."
        );
        ReflectionTestUtils.setField(message, "id", 102L);
        ReflectionTestUtils.setField(
                message,
                "createdAt",
                LocalDateTime.of(2026, 9, 8, 12, 30, 3)
        );
        Recommendation recommendation = Recommendation.create(
                chatRoom.getMember(),
                message
        );
        ReflectionTestUtils.setField(recommendation, "id", 15L);
        Product product = createProduct(201L);
        RecommendationResponse recommendationResponse =
                RecommendationResponse.from(
                        recommendation,
                        List.of(RecommendedProductResponse.from(
                                RecommendationProduct.create(
                                        recommendation,
                                        product,
                                        49_000,
                                        "추천 당시 이유"
                                ),
                                true,
                                false
                        ))
                );
        ChatRoomDetailResponse response = ChatRoomDetailResponse.from(
                chatRoom,
                List.of(ChatMessageDetailResponse.from(
                        message,
                        recommendationResponse
                )),
                102L,
                true
        );
        given(chatService.getChatRoomDetail(1L, 123L, null, 20))
                .willReturn(response);

        mockMvc.perform(get("/api/v1/chat-rooms/123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.chatRoomId").value(123))
                .andExpect(jsonPath("$.data.title")
                        .value("가을 출근용 니트 추천"))
                .andExpect(jsonPath("$.data.messages[0].messageId").value(102))
                .andExpect(jsonPath("$.data.messages[0].senderType").value("AI"))
                .andExpect(jsonPath("$.data.messages[0].generationStatus")
                        .isEmpty())
                .andExpect(jsonPath("$.data.messages[0].recommendation.recommendationId")
                        .value(15))
                .andExpect(jsonPath("$.data.messages[0].recommendation.products[0].productId")
                        .value(201))
                .andExpect(jsonPath("$.data.messages[0].recommendation.products[0].currentPrice")
                        .value(49000))
                .andExpect(jsonPath("$.data.messages[0].recommendation.products[0].recommendedReason")
                        .value("추천 당시 이유"))
                .andExpect(jsonPath("$.data.messages[0].recommendation.products[0].isWishlisted")
                        .value(true))
                .andExpect(jsonPath("$.data.messages[0].recommendation.products[0].isFittingCandidate")
                        .value(false))
                .andExpect(jsonPath("$.data.nextCursor").value(102))
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andExpect(jsonPath("$.message")
                        .value("요청이 성공적으로 처리되었습니다."));

        verify(chatService).getChatRoomDetail(1L, 123L, null, 20);
    }

    @Test
    @DisplayName("Cursor와 조회 크기로 특정 채팅방의 이전 메시지를 조회한다")
    void getPreviousChatRoomDetailPage() throws Exception {
        ChatRoom chatRoom = createChatRoom(123L, LocalDateTime.now());
        given(chatService.getChatRoomDetail(1L, 123L, 101L, 10))
                .willReturn(ChatRoomDetailResponse.from(
                        chatRoom,
                        List.of(),
                        null,
                        false
                ));

        mockMvc.perform(get("/api/v1/chat-rooms/123")
                        .queryParam("cursor", "101")
                        .queryParam("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messages").isEmpty())
                .andExpect(jsonPath("$.data.nextCursor").isEmpty())
                .andExpect(jsonPath("$.data.hasNext").value(false));

        verify(chatService).getChatRoomDetail(1L, 123L, 101L, 10);
    }

    @Test
    @DisplayName("상세 조회 Cursor가 유효하지 않으면 400 Bad Request를 반환한다")
    void rejectInvalidDetailCursor() throws Exception {
        given(chatService.getChatRoomDetail(1L, 123L, 999L, 20))
                .willThrow(new BusinessException(
                        ErrorCode.INVALID_PAGINATION_PARAMETER
                ));

        mockMvc.perform(get("/api/v1/chat-rooms/123")
                        .queryParam("cursor", "999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_PAGINATION_PARAMETER"));
    }

    @Test
    @DisplayName("AI 응답 생성 중에는 상태와 null 메시지를 반환한다")
    void getGeneratingStatus() throws Exception {
        given(chatService.getGenerationStatus(1L, 123L, 101L))
                .willReturn(new ChatGenerationStatusResponse(
                        ChatGenerationStatus.GENERATING,
                        null
                ));

        mockMvc.perform(get(
                        "/api/v1/chat-rooms/123/messages/101/status"
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.generationStatus")
                        .value("GENERATING"))
                .andExpect(jsonPath("$.data.message").isEmpty())
                .andExpect(jsonPath("$.message")
                        .value("요청이 성공적으로 처리되었습니다."));

        verify(chatService).getGenerationStatus(1L, 123L, 101L);
    }

    @Test
    @DisplayName("AI 응답 생성 완료 시 최종 AI 메시지를 반환한다")
    void getCompletedStatus() throws Exception {
        ChatRoom chatRoom = createChatRoom(123L, LocalDateTime.now());
        ChatMessage aiMessage = ChatMessage.createAiText(
                chatRoom,
                "출근할 때 입기 좋은 니트를 추천해드릴게요."
        );
        ReflectionTestUtils.setField(aiMessage, "id", 102L);
        ReflectionTestUtils.setField(
                aiMessage,
                "createdAt",
                LocalDateTime.of(2026, 9, 8, 12, 30, 3)
        );
        ChatMessageDetailResponse messageResponse =
                ChatMessageDetailResponse.from(aiMessage, null);
        given(chatService.getGenerationStatus(1L, 123L, 101L))
                .willReturn(new ChatGenerationStatusResponse(
                        ChatGenerationStatus.COMPLETED,
                        messageResponse
                ));

        mockMvc.perform(get(
                        "/api/v1/chat-rooms/123/messages/101/status"
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.generationStatus")
                        .value("COMPLETED"))
                .andExpect(jsonPath("$.data.message.messageId").value(102))
                .andExpect(jsonPath("$.data.message.senderType").value("AI"))
                .andExpect(jsonPath("$.data.message.content")
                        .value("출근할 때 입기 좋은 니트를 추천해드릴게요."))
                .andExpect(jsonPath("$.data.message.generationStatus")
                        .isEmpty())
                .andExpect(jsonPath("$.data.message.recommendation").isEmpty())
                .andExpect(jsonPath("$.data.message.createdAt")
                        .value("2026-09-08T12:30:03"));

        verify(chatService).getGenerationStatus(1L, 123L, 101L);
    }

    @Test
    @DisplayName("상태 조회 대상 메시지가 없으면 404 Not Found를 반환한다")
    void rejectMissingGenerationTargetMessage() throws Exception {
        given(chatService.getGenerationStatus(1L, 123L, 999L))
                .willThrow(new BusinessException(
                        ErrorCode.CHAT_MESSAGE_NOT_FOUND
                ));

        mockMvc.perform(get(
                        "/api/v1/chat-rooms/123/messages/999/status"
                ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("CHAT_MESSAGE_NOT_FOUND"))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.message")
                        .value("채팅 메시지를 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("새 대화를 시작하면 201 Created를 반환한다")
    void createChatRoom() throws Exception {
        given(chatService.createChatRoom(any(), any()))
                .willReturn(new ChatRoomCreateResponse(
                        10L,
                        100L,
                        LocalDateTime.of(2026, 9, 24, 16, 50)
                ));

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
                .andExpect(jsonPath("$.data.createdAt")
                        .value("2026-09-24T16:50:00"))
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
                        "검은색으로 추천해줘",
                        LocalDateTime.of(2026, 9, 24, 16, 55)
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
                .andExpect(jsonPath("$.data.content").value("검은색으로 추천해줘"))
                .andExpect(jsonPath("$.data.createdAt")
                        .value("2026-09-24T16:55:00"));

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

    @Test
    @DisplayName("채팅방 제목을 수정하면 200 OK를 반환한다")
    void updateTitle() throws Exception {
        given(chatService.updateTitle(any(), any(), any()))
                .willReturn(new ChatRoomTitleUpdateResponse(
                        10L,
                        "가을 출근용 니트 추천"
                ));

        mockMvc.perform(patch("/api/v1/chat-rooms/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "가을 출근용 니트 추천"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.chatRoomId").value(10))
                .andExpect(jsonPath("$.data.title")
                        .value("가을 출근용 니트 추천"))
                .andExpect(jsonPath("$.message")
                        .value("요청이 성공적으로 처리되었습니다."));

        verify(chatService).updateTitle(eq(1L), eq(10L), any());
    }

    @Test
    @DisplayName("채팅방 제목이 공백이면 400 Bad Request를 반환한다")
    void rejectBlankTitle() throws Exception {
        mockMvc.perform(patch("/api/v1/chat-rooms/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("채팅방을 삭제하면 200 OK를 반환한다")
    void deleteChatRoom() throws Exception {
        mockMvc.perform(delete("/api/v1/chat-rooms/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.message")
                        .value("요청이 성공적으로 처리되었습니다."));

        verify(chatService).deleteChatRoom(1L, 10L);
    }

    @Test
    @DisplayName("없는 채팅방을 수정하면 404 Not Found를 반환한다")
    void rejectMissingChatRoomTitleUpdate() throws Exception {
        given(chatService.updateTitle(any(), any(), any()))
                .willThrow(new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        mockMvc.perform(patch("/api/v1/chat-rooms/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "가을 출근용 니트 추천"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CHAT_ROOM_NOT_FOUND"));
    }

    @Test
    @DisplayName("다른 회원의 채팅방을 삭제하면 403 Forbidden을 반환한다")
    void rejectOtherMembersChatRoomDeletion() throws Exception {
        org.mockito.Mockito.doThrow(
                        new BusinessException(ErrorCode.CHAT_ROOM_ACCESS_DENIED)
                )
                .when(chatService)
                .deleteChatRoom(1L, 10L);

        mockMvc.perform(delete("/api/v1/chat-rooms/10"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CHAT_ROOM_ACCESS_DENIED"));
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

    private ChatRoom createChatRoom(Long chatRoomId, LocalDateTime lastMessageAt) {
        Member member = Member.create("member@lookddak.com", "encoded-password");
        ChatRoom chatRoom = ChatRoom.create(
                member,
                "가을 출근용 니트 추천",
                ChatSourceType.GENERAL,
                lastMessageAt
        );
        ReflectionTestUtils.setField(chatRoom, "id", chatRoomId);
        return chatRoom;
    }

    private Product createProduct(Long productId) {
        Product product = Product.create(
                "product-" + productId,
                "에센셜 램스울 크루넥",
                "https://image.lookddak.com/products/201.jpg",
                59_000,
                "네이비",
                ProductItemType.TOP,
                "https://shop.lookddak.com/products/201"
        );
        ReflectionTestUtils.setField(product, "id", productId);
        return product;
    }
}
