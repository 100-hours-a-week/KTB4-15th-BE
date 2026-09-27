package com.ktb.lookddak.domain.chat.service;

import com.ktb.lookddak.domain.chat.dto.ChatMessageDetailResponse;
import com.ktb.lookddak.domain.chat.dto.ChatRoomDetailResponse;
import com.ktb.lookddak.domain.chat.dto.RecommendedProductResponse;
import com.ktb.lookddak.domain.chat.entity.ChatMessage;
import com.ktb.lookddak.domain.chat.entity.ChatRoom;
import com.ktb.lookddak.domain.chat.entity.ChatSourceType;
import com.ktb.lookddak.domain.chat.repository.ChatMessageRepository;
import com.ktb.lookddak.domain.chat.repository.ChatRoomRepository;
import com.ktb.lookddak.domain.fitting.repository.FittingCandidateRepository;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.member.repository.MemberRepository;
import com.ktb.lookddak.domain.product.entity.Product;
import com.ktb.lookddak.domain.product.entity.ProductItemType;
import com.ktb.lookddak.domain.recommendation.entity.Recommendation;
import com.ktb.lookddak.domain.recommendation.entity.RecommendationProduct;
import com.ktb.lookddak.domain.recommendation.repository.RecommendationProductRepository;
import com.ktb.lookddak.domain.recommendation.repository.RecommendationRepository;
import com.ktb.lookddak.domain.wishlist.repository.WishlistRepository;
import com.ktb.lookddak.global.exception.BusinessException;
import com.ktb.lookddak.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ChatRoomDetailServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private RecommendationRepository recommendationRepository;

    @Mock
    private RecommendationProductRepository recommendationProductRepository;

    @Mock
    private WishlistRepository wishlistRepository;

    @Mock
    private FittingCandidateRepository fittingCandidateRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(
                memberRepository,
                chatRoomRepository,
                chatMessageRepository,
                recommendationRepository,
                recommendationProductRepository,
                wishlistRepository,
                fittingCandidateRepository,
                eventPublisher
        );
    }

    @Test
    @DisplayName("최근 메시지와 추천 상품 상태를 시간순으로 조립한다")
    void getFirstChatRoomDetailPage() {
        Member member = createMember(1L);
        ChatRoom chatRoom = createChatRoom(123L, member);
        ChatMessage userMessage = createCompletedUserMessage(
                101L,
                chatRoom,
                "가을 출근용 니트 추천해줘"
        );
        ChatMessage recommendationMessage = createRecommendationMessage(
                102L,
                chatRoom,
                "출근할 때 입기 좋은 니트를 추천해드릴게요."
        );
        ChatMessage aiTextMessage = createAiTextMessage(
                103L,
                chatRoom,
                "원하는 색상이 있으신가요?"
        );
        ChatMessage hasNextCheckMessage = createCompletedUserMessage(
                100L,
                chatRoom,
                "더 오래된 메시지"
        );
        Recommendation recommendation = Recommendation.create(
                member,
                recommendationMessage
        );
        ReflectionTestUtils.setField(recommendation, "id", 15L);
        Product firstProduct = createProduct(201L, "첫 번째 추천 상품");
        Product secondProduct = createProduct(202L, "두 번째 추천 상품");
        RecommendationProduct firstRecommendationProduct =
                RecommendationProduct.create(
                        recommendation,
                        firstProduct,
                        49_000,
                        "첫 번째 추천 이유"
                );
        RecommendationProduct secondRecommendationProduct =
                RecommendationProduct.create(
                        recommendation,
                        secondProduct,
                        69_000,
                        "두 번째 추천 이유"
                );

        given(chatRoomRepository.findActiveById(123L))
                .willReturn(Optional.of(chatRoom));
        given(chatMessageRepository.findFirstPage(eq(123L), any(Pageable.class)))
                .willReturn(List.of(
                        aiTextMessage,
                        recommendationMessage,
                        userMessage,
                        hasNextCheckMessage
                ));
        given(recommendationRepository.findAllByMessageIdIn(List.of(102L)))
                .willReturn(List.of(recommendation));
        given(recommendationProductRepository
                .findAllWithProductByRecommendationIdIn(List.of(15L)))
                .willReturn(List.of(
                        firstRecommendationProduct,
                        secondRecommendationProduct
                ));
        given(wishlistRepository.findProductIdsByMemberIdAndProductIdIn(
                eq(1L),
                eq(Set.of(201L, 202L))
        )).willReturn(Set.of(201L));
        given(fittingCandidateRepository
                .findProductIdsByMemberIdAndProductIdIn(
                        eq(1L),
                        eq(Set.of(201L, 202L))
                )).willReturn(Set.of(202L));

        ChatRoomDetailResponse response = chatService.getChatRoomDetail(
                1L,
                123L,
                null,
                3
        );

        assertThat(response.getChatRoomId()).isEqualTo(123L);
        assertThat(response.getTitle()).isEqualTo("가을 출근용 니트 추천해줘");
        assertThat(response.getMessages())
                .extracting(ChatMessageDetailResponse::getMessageId)
                .containsExactly(101L, 102L, 103L);
        assertThat(response.getNextCursor()).isEqualTo(101L);
        assertThat(response.isHasNext()).isTrue();

        ChatMessageDetailResponse recommendationResponse =
                response.getMessages().get(1);
        assertThat(recommendationResponse.getRecommendation()).isNotNull();
        assertThat(recommendationResponse.getRecommendation().getProducts())
                .extracting(RecommendedProductResponse::getProductId)
                .containsExactly(201L, 202L);
        assertThat(recommendationResponse.getRecommendation().getProducts().get(0)
                .isWishlisted()).isTrue();
        assertThat(recommendationResponse.getRecommendation().getProducts().get(0)
                .isFittingCandidate()).isFalse();
        assertThat(recommendationResponse.getRecommendation().getProducts().get(1)
                .isWishlisted()).isFalse();
        assertThat(recommendationResponse.getRecommendation().getProducts().get(1)
                .isFittingCandidate()).isTrue();
        assertThat(response.getMessages().get(0).getRecommendation()).isNull();
        assertThat(response.getMessages().get(2).getRecommendation()).isNull();
    }

    @Test
    @DisplayName("유효한 Cursor로 이전 메시지 마지막 페이지를 조회한다")
    void getPreviousChatRoomDetailPage() {
        Member member = createMember(1L);
        ChatRoom chatRoom = createChatRoom(123L, member);
        ChatMessage olderMessage = createCompletedUserMessage(
                99L,
                chatRoom,
                "이전 메시지"
        );
        given(chatRoomRepository.findActiveById(123L))
                .willReturn(Optional.of(chatRoom));
        given(chatMessageRepository.existsByIdAndChatRoomId(101L, 123L))
                .willReturn(true);
        given(chatMessageRepository.findPreviousPage(
                eq(123L),
                eq(101L),
                any(Pageable.class)
        )).willReturn(List.of(olderMessage));

        ChatRoomDetailResponse response = chatService.getChatRoomDetail(
                1L,
                123L,
                101L,
                20
        );

        assertThat(response.getMessages())
                .extracting(ChatMessageDetailResponse::getMessageId)
                .containsExactly(99L);
        assertThat(response.getNextCursor()).isNull();
        assertThat(response.isHasNext()).isFalse();
        verifyNoInteractions(
                recommendationRepository,
                recommendationProductRepository,
                wishlistRepository,
                fittingCandidateRepository
        );
    }

    @Test
    @DisplayName("현재 채팅방 메시지가 아닌 Cursor는 사용할 수 없다")
    void rejectInvalidMessageCursor() {
        Member member = createMember(1L);
        ChatRoom chatRoom = createChatRoom(123L, member);
        given(chatRoomRepository.findActiveById(123L))
                .willReturn(Optional.of(chatRoom));
        given(chatMessageRepository.existsByIdAndChatRoomId(999L, 123L))
                .willReturn(false);

        assertThatThrownBy(() -> chatService.getChatRoomDetail(
                1L,
                123L,
                999L,
                20
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_PAGINATION_PARAMETER)
        );

        verify(chatMessageRepository, never()).findPreviousPage(
                any(),
                any(),
                any()
        );
    }

    @Test
    @DisplayName("존재하지 않거나 삭제된 채팅방은 조회할 수 없다")
    void rejectMissingChatRoom() {
        given(chatRoomRepository.findActiveById(123L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.getChatRoomDetail(
                1L,
                123L,
                null,
                20
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.CHAT_ROOM_NOT_FOUND)
        );
    }

    @Test
    @DisplayName("다른 회원의 채팅방은 조회할 수 없다")
    void rejectOtherMembersChatRoom() {
        ChatRoom chatRoom = createChatRoom(123L, createMember(2L));
        given(chatRoomRepository.findActiveById(123L))
                .willReturn(Optional.of(chatRoom));

        assertThatThrownBy(() -> chatService.getChatRoomDetail(
                1L,
                123L,
                null,
                20
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.CHAT_ROOM_ACCESS_DENIED)
        );
    }

    private Member createMember(Long id) {
        Member member = Member.create("member" + id + "@lookddak.com", "encoded");
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private ChatRoom createChatRoom(Long id, Member member) {
        ChatRoom chatRoom = ChatRoom.create(
                member,
                "가을 출근용 니트 추천해줘",
                ChatSourceType.GENERAL,
                LocalDateTime.now()
        );
        ReflectionTestUtils.setField(chatRoom, "id", id);
        return chatRoom;
    }

    private ChatMessage createCompletedUserMessage(
            Long id,
            ChatRoom chatRoom,
            String content
    ) {
        ChatMessage message = ChatMessage.createUserText(chatRoom, content);
        message.completeGeneration();
        setMessageFields(message, id);
        return message;
    }

    private ChatMessage createRecommendationMessage(
            Long id,
            ChatRoom chatRoom,
            String content
    ) {
        ChatMessage message = ChatMessage.createAiRecommendation(chatRoom, content);
        setMessageFields(message, id);
        return message;
    }

    private ChatMessage createAiTextMessage(
            Long id,
            ChatRoom chatRoom,
            String content
    ) {
        ChatMessage message = ChatMessage.createAiText(chatRoom, content);
        setMessageFields(message, id);
        return message;
    }

    private void setMessageFields(ChatMessage message, Long id) {
        ReflectionTestUtils.setField(message, "id", id);
        ReflectionTestUtils.setField(message, "createdAt", LocalDateTime.now());
    }

    private Product createProduct(Long id, String name) {
        Product product = Product.create(
                "product-" + id,
                name,
                "https://image.lookddak.com/products/" + id + ".jpg",
                59_000,
                "네이비",
                ProductItemType.TOP,
                "https://shop.lookddak.com/products/" + id
        );
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }
}
