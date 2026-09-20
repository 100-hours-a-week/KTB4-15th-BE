package com.ktb.lookddak.domain.chat;

import com.jayway.jsonpath.JsonPath;
import com.ktb.lookddak.domain.chat.entity.ChatGenerationStatus;
import com.ktb.lookddak.domain.chat.entity.ChatMessage;
import com.ktb.lookddak.domain.chat.entity.ChatRoom;
import com.ktb.lookddak.domain.chat.entity.ChatSenderType;
import com.ktb.lookddak.domain.chat.entity.ChatSourceType;
import com.ktb.lookddak.domain.chat.repository.ChatMessageRepository;
import com.ktb.lookddak.domain.chat.repository.ChatRoomRepository;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.member.repository.MemberRepository;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ChatIntegrationTest {

    private static final String EMAIL = "chat-integration@lookddak.com";
    private static final String PASSWORD = "Test1234!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private Member member;

    @BeforeEach
    void setUp() {
        member = memberRepository.saveAndFlush(Member.create(
                EMAIL,
                passwordEncoder.encode(PASSWORD)
        ));
    }

    @Test
    @DisplayName("인증된 회원이 새 대화를 시작하고 AI 응답 완료 후 메시지를 전송한다")
    void createChatRoomAndMessage() throws Exception {
        String accessToken = loginAndGetAccessToken();

        MvcResult createRoomResult = mockMvc.perform(post("/api/v1/chat-rooms")
                        .cookie(new Cookie("accessToken", accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "5만원대 캐주얼 니트 추천해줘",
                                  "sourceType": "WISHLIST"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CREATED"))
                .andExpect(jsonPath("$.data.chatRoomId").isNumber())
                .andExpect(jsonPath("$.data.messageId").isNumber())
                .andReturn();

        String responseBody = createRoomResult.getResponse().getContentAsString();
        Long chatRoomId = ((Number) JsonPath.read(
                responseBody,
                "$.data.chatRoomId"
        )).longValue();
        Long firstMessageId = ((Number) JsonPath.read(
                responseBody,
                "$.data.messageId"
        )).longValue();

        entityManager.flush();
        entityManager.clear();

        ChatRoom savedChatRoom = chatRoomRepository.findById(chatRoomId).orElseThrow();
        ChatMessage firstMessage = chatMessageRepository
                .findById(firstMessageId)
                .orElseThrow();
        assertThat(savedChatRoom.getMember().getId()).isEqualTo(member.getId());
        assertThat(savedChatRoom.getSourceType()).isEqualTo(ChatSourceType.WISHLIST);
        assertThat(firstMessage.getGenerationStatus())
                .isEqualTo(ChatGenerationStatus.GENERATING);

        mockMvc.perform(post("/api/v1/chat-rooms/{chatRoomId}/messages", chatRoomId)
                        .cookie(new Cookie("accessToken", accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "검은색으로 추천해줘"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AI_RESPONSE_GENERATING"));

        jdbcTemplate.update(
                "update chat_message set generation_status = ? where id = ?",
                "COMPLETED",
                firstMessageId
        );
        entityManager.clear();

        mockMvc.perform(post("/api/v1/chat-rooms/{chatRoomId}/messages", chatRoomId)
                        .cookie(new Cookie("accessToken", accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "검은색으로 추천해줘"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CREATED"))
                .andExpect(jsonPath("$.data.chatRoomId").value(chatRoomId))
                .andExpect(jsonPath("$.data.messageId").isNumber())
                .andExpect(jsonPath("$.data.content").value("검은색으로 추천해줘"));

        assertThat(chatMessageRepository
                .existsByChatRoomIdAndSenderTypeAndGenerationStatus(
                        chatRoomId,
                        ChatSenderType.USER,
                        ChatGenerationStatus.GENERATING
                )).isTrue();
    }

    @Test
    @DisplayName("인증하지 않은 회원은 새 대화를 시작할 수 없다")
    void rejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(post("/api/v1/chat-rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "옷을 추천해줘",
                                  "sourceType": "GENERAL"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/v1/chat-rooms"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("인증된 회원이 채팅방 제목을 수정하고 소프트 삭제한다")
    void updateAndDeleteChatRoom() throws Exception {
        String accessToken = loginAndGetAccessToken();
        MvcResult createRoomResult = mockMvc.perform(post("/api/v1/chat-rooms")
                        .cookie(new Cookie("accessToken", accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "가을에 입을 니트를 추천해줘",
                                  "sourceType": "GENERAL"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createRoomResult.getResponse().getContentAsString();
        Long chatRoomId = ((Number) JsonPath.read(
                responseBody,
                "$.data.chatRoomId"
        )).longValue();
        Long messageId = ((Number) JsonPath.read(
                responseBody,
                "$.data.messageId"
        )).longValue();

        mockMvc.perform(patch("/api/v1/chat-rooms/{chatRoomId}", chatRoomId)
                        .cookie(new Cookie("accessToken", accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "  가을 출근용 니트 추천  "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.chatRoomId").value(chatRoomId))
                .andExpect(jsonPath("$.data.title")
                        .value("가을 출근용 니트 추천"));

        mockMvc.perform(delete("/api/v1/chat-rooms/{chatRoomId}", chatRoomId)
                        .cookie(new Cookie("accessToken", accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data").isEmpty());

        entityManager.flush();
        entityManager.clear();

        ChatRoom deletedChatRoom = chatRoomRepository
                .findById(chatRoomId)
                .orElseThrow();
        assertThat(deletedChatRoom.getTitle())
                .isEqualTo("가을 출근용 니트 추천");
        assertThat(deletedChatRoom.isDeleted()).isTrue();
        assertThat(deletedChatRoom.getDeletedAt()).isNotNull();
        assertThat(chatMessageRepository.findById(messageId)).isPresent();
        assertThat(chatRoomRepository.findActiveByIdForUpdate(chatRoomId))
                .isEmpty();

        mockMvc.perform(patch("/api/v1/chat-rooms/{chatRoomId}", chatRoomId)
                        .cookie(new Cookie("accessToken", accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "다시 수정할 제목"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CHAT_ROOM_NOT_FOUND"));

        mockMvc.perform(post("/api/v1/chat-rooms/{chatRoomId}/messages", chatRoomId)
                        .cookie(new Cookie("accessToken", accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "삭제된 방에 메시지 전송"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CHAT_ROOM_NOT_FOUND"));
    }

    @Test
    @DisplayName("인증된 회원의 활성 채팅방을 Cursor 기반으로 조회한다")
    void getChatRoomsWithCursor() throws Exception {
        String accessToken = loginAndGetAccessToken();
        LocalDateTime sameTime = LocalDateTime.of(2026, 9, 21, 14, 0);
        ChatRoom olderRoom = saveChatRoom(
                member,
                "오래된 채팅방",
                sameTime.minusHours(1)
        );
        ChatRoom sameTimeLowerIdRoom = saveChatRoom(
                member,
                "같은 시각 낮은 ID",
                sameTime
        );
        ChatRoom sameTimeHigherIdRoom = saveChatRoom(
                member,
                "같은 시각 높은 ID",
                sameTime
        );
        ChatRoom deletedRoom = saveChatRoom(
                member,
                "삭제된 채팅방",
                sameTime.plusHours(1)
        );
        deletedRoom.delete(LocalDateTime.now());

        Member otherMember = memberRepository.saveAndFlush(Member.create(
                "other-chat-integration@lookddak.com",
                passwordEncoder.encode(PASSWORD)
        ));
        ChatRoom otherRoom = saveChatRoom(
                otherMember,
                "다른 회원 채팅방",
                sameTime.plusHours(2)
        );
        chatRoomRepository.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/v1/chat-rooms")
                        .cookie(new Cookie("accessToken", accessToken))
                        .queryParam("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].chatRoomId")
                        .value(sameTimeHigherIdRoom.getId()))
                .andExpect(jsonPath("$.data.items[1].chatRoomId")
                        .value(sameTimeLowerIdRoom.getId()))
                .andExpect(jsonPath("$.data.nextCursor")
                        .value(sameTimeLowerIdRoom.getId()))
                .andExpect(jsonPath("$.data.hasNext").value(true));

        mockMvc.perform(get("/api/v1/chat-rooms")
                        .cookie(new Cookie("accessToken", accessToken))
                        .queryParam(
                                "cursor",
                                sameTimeLowerIdRoom.getId().toString()
                        )
                        .queryParam("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].chatRoomId")
                        .value(olderRoom.getId()))
                .andExpect(jsonPath("$.data.nextCursor").isEmpty())
                .andExpect(jsonPath("$.data.hasNext").value(false));

        mockMvc.perform(get("/api/v1/chat-rooms")
                        .cookie(new Cookie("accessToken", accessToken))
                        .queryParam("cursor", deletedRoom.getId().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_PAGINATION_PARAMETER"));

        mockMvc.perform(get("/api/v1/chat-rooms")
                        .cookie(new Cookie("accessToken", accessToken))
                        .queryParam("cursor", otherRoom.getId().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_PAGINATION_PARAMETER"));
    }

    private String loginAndGetAccessToken() throws Exception {
        List<String> setCookieHeaders = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeaders(HttpHeaders.SET_COOKIE);

        String prefix = "accessToken=";
        String accessTokenCookie = setCookieHeaders.stream()
                .filter(header -> header.startsWith(prefix))
                .findFirst()
                .orElseThrow();

        return accessTokenCookie.substring(
                prefix.length(),
                accessTokenCookie.indexOf(';')
        );
    }

    private ChatRoom saveChatRoom(
            Member owner,
            String title,
            LocalDateTime lastMessageAt
    ) {
        return chatRoomRepository.saveAndFlush(ChatRoom.create(
                owner,
                title,
                ChatSourceType.GENERAL,
                lastMessageAt
        ));
    }
}
