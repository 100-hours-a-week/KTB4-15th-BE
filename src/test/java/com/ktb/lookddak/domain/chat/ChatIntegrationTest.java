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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
}
