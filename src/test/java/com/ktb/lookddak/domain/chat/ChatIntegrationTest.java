package com.ktb.lookddak.domain.chat;

import com.jayway.jsonpath.JsonPath;
import com.ktb.lookddak.domain.chat.entity.ChatGenerationStatus;
import com.ktb.lookddak.domain.chat.entity.ChatMessage;
import com.ktb.lookddak.domain.chat.entity.ChatRoom;
import com.ktb.lookddak.domain.chat.entity.ChatSenderType;
import com.ktb.lookddak.domain.chat.entity.ChatSourceType;
import com.ktb.lookddak.domain.chat.repository.ChatMessageRepository;
import com.ktb.lookddak.domain.chat.repository.ChatRoomRepository;
import com.ktb.lookddak.domain.fitting.entity.FittingCandidate;
import com.ktb.lookddak.domain.fitting.repository.FittingCandidateRepository;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.member.repository.MemberRepository;
import com.ktb.lookddak.domain.product.entity.Product;
import com.ktb.lookddak.domain.product.entity.ProductItemType;
import com.ktb.lookddak.domain.product.repository.ProductRepository;
import com.ktb.lookddak.domain.recommendation.entity.Recommendation;
import com.ktb.lookddak.domain.recommendation.entity.RecommendationProduct;
import com.ktb.lookddak.domain.recommendation.repository.RecommendationProductRepository;
import com.ktb.lookddak.domain.recommendation.repository.RecommendationRepository;
import com.ktb.lookddak.domain.wishlist.entity.Wishlist;
import com.ktb.lookddak.domain.wishlist.repository.WishlistRepository;
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
    private ProductRepository productRepository;

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Autowired
    private RecommendationProductRepository recommendationProductRepository;

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private FittingCandidateRepository fittingCandidateRepository;

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
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty())
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
                .andExpect(jsonPath("$.data.content").value("검은색으로 추천해줘"))
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty());

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

    @Test
    @DisplayName("채팅방 상세를 메시지 커서와 추천 상품 상태를 포함해 조회한다")
    void getChatRoomDetailWithRecommendationProducts() throws Exception {
        String accessToken = loginAndGetAccessToken();
        ChatRoom chatRoom = saveChatRoom(
                member,
                "가을 니트 추천",
                LocalDateTime.now()
        );

        ChatMessage oldestMessage = saveCompletedUserMessage(
                chatRoom,
                "처음 보낸 메시지"
        );
        ChatMessage userMessage = saveCompletedUserMessage(
                chatRoom,
                "검은색 니트를 추천해줘"
        );
        ChatMessage recommendationMessage = chatMessageRepository.saveAndFlush(
                ChatMessage.createAiRecommendation(
                        chatRoom,
                        "조건에 맞는 상품을 추천해드릴게요."
                )
        );
        ChatMessage aiTextMessage = chatMessageRepository.saveAndFlush(
                ChatMessage.createAiText(
                        chatRoom,
                        "마음에 드는 상품을 골라보세요."
                )
        );

        Recommendation recommendation = recommendationRepository.saveAndFlush(
                Recommendation.create(member, recommendationMessage)
        );
        Product firstProduct = saveProduct(
                "에센셜 검정 니트",
                "https://example.com/knit.jpg",
                49000,
                "BLACK",
                ProductItemType.TOP,
                "https://example.com/products/knit"
        );
        Product secondProduct = saveProduct(
                "와이드 데님 팬츠",
                "https://example.com/denim.jpg",
                59000,
                "BLUE",
                ProductItemType.BOTTOM,
                "https://example.com/products/denim"
        );
        recommendationProductRepository.saveAndFlush(
                RecommendationProduct.create(recommendation, firstProduct)
        );
        recommendationProductRepository.saveAndFlush(
                RecommendationProduct.create(recommendation, secondProduct)
        );
        wishlistRepository.saveAndFlush(Wishlist.create(member, firstProduct));
        fittingCandidateRepository.saveAndFlush(
                FittingCandidate.create(member, secondProduct)
        );
        entityManager.clear();

        mockMvc.perform(get("/api/v1/chat-rooms/{chatRoomId}", chatRoom.getId())
                        .cookie(new Cookie("accessToken", accessToken))
                        .queryParam("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.chatRoomId").value(chatRoom.getId()))
                .andExpect(jsonPath("$.data.title").value("가을 니트 추천"))
                .andExpect(jsonPath("$.data.messages.length()").value(3))
                .andExpect(jsonPath("$.data.messages[0].messageId")
                        .value(userMessage.getId()))
                .andExpect(jsonPath("$.data.messages[0].senderType")
                        .value("USER"))
                .andExpect(jsonPath("$.data.messages[0].generationStatus")
                        .value("COMPLETED"))
                .andExpect(jsonPath("$.data.messages[0].recommendation").isEmpty())
                .andExpect(jsonPath("$.data.messages[1].messageId")
                        .value(recommendationMessage.getId()))
                .andExpect(jsonPath("$.data.messages[1].senderType")
                        .value("AI"))
                .andExpect(jsonPath("$.data.messages[1].generationStatus").isEmpty())
                .andExpect(jsonPath("$.data.messages[1].recommendation.recommendationId")
                        .value(recommendation.getId()))
                .andExpect(jsonPath("$.data.messages[1].recommendation.products.length()")
                        .value(2))
                .andExpect(jsonPath("$.data.messages[1].recommendation.products[0].productId")
                        .value(firstProduct.getId()))
                .andExpect(jsonPath("$.data.messages[1].recommendation.products[0].productName")
                        .value("에센셜 검정 니트"))
                .andExpect(jsonPath("$.data.messages[1].recommendation.products[0].currentPrice")
                        .value(49000))
                .andExpect(jsonPath("$.data.messages[1].recommendation.products[0].itemType")
                        .value("TOP"))
                .andExpect(jsonPath("$.data.messages[1].recommendation.products[0].isWishlisted")
                        .value(true))
                .andExpect(jsonPath("$.data.messages[1].recommendation.products[0].isFittingCandidate")
                        .value(false))
                .andExpect(jsonPath("$.data.messages[1].recommendation.products[1].productId")
                        .value(secondProduct.getId()))
                .andExpect(jsonPath("$.data.messages[1].recommendation.products[1].isWishlisted")
                        .value(false))
                .andExpect(jsonPath("$.data.messages[1].recommendation.products[1].isFittingCandidate")
                        .value(true))
                .andExpect(jsonPath("$.data.messages[2].messageId")
                        .value(aiTextMessage.getId()))
                .andExpect(jsonPath("$.data.messages[2].recommendation").isEmpty())
                .andExpect(jsonPath("$.data.nextCursor").value(userMessage.getId()))
                .andExpect(jsonPath("$.data.hasNext").value(true));

        mockMvc.perform(get("/api/v1/chat-rooms/{chatRoomId}", chatRoom.getId())
                        .cookie(new Cookie("accessToken", accessToken))
                        .queryParam("cursor", userMessage.getId().toString())
                        .queryParam("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messages.length()").value(1))
                .andExpect(jsonPath("$.data.messages[0].messageId")
                        .value(oldestMessage.getId()))
                .andExpect(jsonPath("$.data.nextCursor").isEmpty())
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    @DisplayName("채팅방 상세 조회 시 소유권과 활성 상태 및 커서를 검증한다")
    void validateChatRoomDetailRequest() throws Exception {
        String accessToken = loginAndGetAccessToken();
        Member otherMember = memberRepository.saveAndFlush(Member.create(
                "other-detail-integration@lookddak.com",
                passwordEncoder.encode(PASSWORD)
        ));
        ChatRoom otherRoom = saveChatRoom(
                otherMember,
                "다른 회원 채팅방",
                LocalDateTime.now()
        );
        ChatRoom deletedRoom = saveChatRoom(
                member,
                "삭제된 채팅방",
                LocalDateTime.now()
        );
        deletedRoom.delete(LocalDateTime.now());
        chatRoomRepository.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/v1/chat-rooms/{chatRoomId}", otherRoom.getId())
                        .cookie(new Cookie("accessToken", accessToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CHAT_ROOM_ACCESS_DENIED"));

        mockMvc.perform(get("/api/v1/chat-rooms/{chatRoomId}", deletedRoom.getId())
                        .cookie(new Cookie("accessToken", accessToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CHAT_ROOM_NOT_FOUND"));

        ChatRoom activeRoom = saveChatRoom(
                member,
                "활성 채팅방",
                LocalDateTime.now()
        );
        mockMvc.perform(get("/api/v1/chat-rooms/{chatRoomId}", activeRoom.getId())
                        .cookie(new Cookie("accessToken", accessToken))
                        .queryParam("cursor", Long.toString(Long.MAX_VALUE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_PAGINATION_PARAMETER"));

        mockMvc.perform(get("/api/v1/chat-rooms/{chatRoomId}", activeRoom.getId())
                        .cookie(new Cookie("accessToken", accessToken))
                        .queryParam("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_PAGINATION_PARAMETER"));

        mockMvc.perform(get("/api/v1/chat-rooms/{chatRoomId}", 0)
                        .cookie(new Cookie("accessToken", accessToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));

        mockMvc.perform(get("/api/v1/chat-rooms/{chatRoomId}", activeRoom.getId()))
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

    private ChatMessage saveCompletedUserMessage(
            ChatRoom chatRoom,
            String content
    ) {
        ChatMessage message = ChatMessage.createUserText(chatRoom, content);
        message.completeGeneration();
        return chatMessageRepository.saveAndFlush(message);
    }

    private Product saveProduct(
            String name,
            String imageUrl,
            Integer currentPrice,
            String color,
            ProductItemType itemType,
            String purchaseUrl
    ) {
        return productRepository.saveAndFlush(Product.create(
                name,
                imageUrl,
                currentPrice,
                color,
                itemType,
                purchaseUrl
        ));
    }
}
