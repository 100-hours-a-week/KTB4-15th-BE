package com.ktb.lookddak.domain.chat.service;

import com.ktb.lookddak.domain.chat.dto.ChatGenerationStatusResponse;
import com.ktb.lookddak.domain.chat.entity.ChatGenerationStatus;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ChatGenerationStatusServiceTest {

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

    private ChatService chatService;
    private Member member;
    private ChatRoom chatRoom;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(
                memberRepository,
                chatRoomRepository,
                chatMessageRepository,
                recommendationRepository,
                recommendationProductRepository,
                wishlistRepository,
                fittingCandidateRepository
        );
        member = createMember(1L);
        chatRoom = createChatRoom(123L, member);
    }

    @Test
    @DisplayName("GENERATING 상태는 AI 응답 추가 조회 없이 반환한다")
    void getGeneratingStatus() {
        ChatMessage userMessage = createUserMessage(
                101L,
                ChatGenerationStatus.GENERATING
        );
        givenOwnedChatRoom();
        given(chatMessageRepository.findByIdAndChatRoomId(101L, 123L))
                .willReturn(Optional.of(userMessage));

        ChatGenerationStatusResponse response =
                chatService.getGenerationStatus(1L, 123L, 101L);

        assertThat(response.getGenerationStatus())
                .isEqualTo(ChatGenerationStatus.GENERATING);
        assertThat(response.getMessage()).isNull();
        verify(chatMessageRepository, never())
                .findFirstByChatRoomIdAndIdGreaterThanOrderByIdAsc(
                        123L,
                        101L
                );
        verifyNoRecommendationInteractions();
    }

    @Test
    @DisplayName("FAILED 상태는 AI 응답 추가 조회 없이 반환한다")
    void getFailedStatus() {
        ChatMessage userMessage = createUserMessage(
                101L,
                ChatGenerationStatus.FAILED
        );
        givenOwnedChatRoom();
        given(chatMessageRepository.findByIdAndChatRoomId(101L, 123L))
                .willReturn(Optional.of(userMessage));

        ChatGenerationStatusResponse response =
                chatService.getGenerationStatus(1L, 123L, 101L);

        assertThat(response.getGenerationStatus())
                .isEqualTo(ChatGenerationStatus.FAILED);
        assertThat(response.getMessage()).isNull();
        verify(chatMessageRepository, never())
                .findFirstByChatRoomIdAndIdGreaterThanOrderByIdAsc(
                        123L,
                        101L
                );
        verifyNoRecommendationInteractions();
    }

    @Test
    @DisplayName("COMPLETED 상태는 바로 다음 AI 텍스트 메시지를 반환한다")
    void getCompletedStatusWithAiTextMessage() {
        ChatMessage userMessage = createUserMessage(
                101L,
                ChatGenerationStatus.COMPLETED
        );
        ChatMessage aiMessage = createAiTextMessage(105L);
        givenOwnedChatRoom();
        given(chatMessageRepository.findByIdAndChatRoomId(101L, 123L))
                .willReturn(Optional.of(userMessage));
        given(chatMessageRepository
                .findFirstByChatRoomIdAndIdGreaterThanOrderByIdAsc(
                        123L,
                        101L
                )).willReturn(Optional.of(aiMessage));

        ChatGenerationStatusResponse response =
                chatService.getGenerationStatus(1L, 123L, 101L);

        assertThat(response.getGenerationStatus())
                .isEqualTo(ChatGenerationStatus.COMPLETED);
        assertThat(response.getMessage().getMessageId()).isEqualTo(105L);
        assertThat(response.getMessage().getContent())
                .isEqualTo("최종 AI 텍스트 응답");
        assertThat(response.getMessage().getGenerationStatus()).isNull();
        assertThat(response.getMessage().getRecommendation()).isNull();
        verifyNoRecommendationInteractions();
    }

    @Test
    @DisplayName("COMPLETED 추천 응답에 상품의 찜과 피팅 상태를 포함한다")
    void getCompletedStatusWithRecommendation() {
        ChatMessage userMessage = createUserMessage(
                101L,
                ChatGenerationStatus.COMPLETED
        );
        ChatMessage aiMessage = createAiRecommendationMessage(102L);
        Recommendation recommendation = Recommendation.create(
                member,
                aiMessage
        );
        ReflectionTestUtils.setField(recommendation, "id", 15L);
        Product product = createProduct(201L);
        RecommendationProduct recommendationProduct =
                RecommendationProduct.create(recommendation, product);
        givenOwnedChatRoom();
        given(chatMessageRepository.findByIdAndChatRoomId(101L, 123L))
                .willReturn(Optional.of(userMessage));
        given(chatMessageRepository
                .findFirstByChatRoomIdAndIdGreaterThanOrderByIdAsc(
                        123L,
                        101L
                )).willReturn(Optional.of(aiMessage));
        given(recommendationRepository.findAllByMessageIdIn(List.of(102L)))
                .willReturn(List.of(recommendation));
        given(recommendationProductRepository
                .findAllWithProductByRecommendationIdIn(List.of(15L)))
                .willReturn(List.of(recommendationProduct));
        given(wishlistRepository.findProductIdsByMemberIdAndProductIdIn(
                1L,
                Set.of(201L)
        )).willReturn(Set.of(201L));
        given(fittingCandidateRepository
                .findProductIdsByMemberIdAndProductIdIn(
                        1L,
                        Set.of(201L)
                )).willReturn(Set.of());

        ChatGenerationStatusResponse response =
                chatService.getGenerationStatus(1L, 123L, 101L);

        assertThat(response.getGenerationStatus())
                .isEqualTo(ChatGenerationStatus.COMPLETED);
        assertThat(response.getMessage().getRecommendation()
                .getRecommendationId()).isEqualTo(15L);
        assertThat(response.getMessage().getRecommendation().getProducts())
                .singleElement()
                .satisfies(productResponse -> {
                    assertThat(productResponse.getProductId()).isEqualTo(201L);
                    assertThat(productResponse.isWishlisted()).isTrue();
                    assertThat(productResponse.isFittingCandidate()).isFalse();
                });
    }

    @Test
    @DisplayName("현재 채팅방에 상태 조회 대상 메시지가 없으면 예외가 발생한다")
    void rejectMissingMessage() {
        givenOwnedChatRoom();
        given(chatMessageRepository.findByIdAndChatRoomId(999L, 123L))
                .willReturn(Optional.empty());

        assertErrorCode(
                () -> chatService.getGenerationStatus(1L, 123L, 999L),
                ErrorCode.CHAT_MESSAGE_NOT_FOUND
        );
    }

    @Test
    @DisplayName("AI 메시지는 생성 상태 조회 대상으로 사용할 수 없다")
    void rejectAiMessageAsStatusTarget() {
        ChatMessage aiMessage = createAiTextMessage(102L);
        givenOwnedChatRoom();
        given(chatMessageRepository.findByIdAndChatRoomId(102L, 123L))
                .willReturn(Optional.of(aiMessage));

        assertErrorCode(
                () -> chatService.getGenerationStatus(1L, 123L, 102L),
                ErrorCode.INVALID_INPUT_VALUE
        );
    }

    @Test
    @DisplayName("완료 상태인데 다음 메시지가 없으면 서버 데이터 오류로 처리한다")
    void rejectCompletedStatusWithoutNextMessage() {
        ChatMessage userMessage = createUserMessage(
                101L,
                ChatGenerationStatus.COMPLETED
        );
        givenOwnedChatRoom();
        given(chatMessageRepository.findByIdAndChatRoomId(101L, 123L))
                .willReturn(Optional.of(userMessage));
        given(chatMessageRepository
                .findFirstByChatRoomIdAndIdGreaterThanOrderByIdAsc(
                        123L,
                        101L
                )).willReturn(Optional.empty());

        assertInvalidServerState(() ->
                chatService.getGenerationStatus(1L, 123L, 101L)
        );
    }

    @Test
    @DisplayName("완료 상태의 바로 다음 메시지가 USER이면 서버 데이터 오류로 처리한다")
    void rejectCompletedStatusFollowedByUserMessage() {
        ChatMessage userMessage = createUserMessage(
                101L,
                ChatGenerationStatus.COMPLETED
        );
        ChatMessage nextUserMessage = createUserMessage(
                102L,
                ChatGenerationStatus.GENERATING
        );
        givenOwnedChatRoom();
        given(chatMessageRepository.findByIdAndChatRoomId(101L, 123L))
                .willReturn(Optional.of(userMessage));
        given(chatMessageRepository
                .findFirstByChatRoomIdAndIdGreaterThanOrderByIdAsc(
                        123L,
                        101L
                )).willReturn(Optional.of(nextUserMessage));

        assertInvalidServerState(() ->
                chatService.getGenerationStatus(1L, 123L, 101L)
        );
    }

    @Test
    @DisplayName("추천 메시지에 추천 데이터가 없으면 서버 데이터 오류로 처리한다")
    void rejectRecommendationMessageWithoutRecommendation() {
        ChatMessage userMessage = createUserMessage(
                101L,
                ChatGenerationStatus.COMPLETED
        );
        ChatMessage aiMessage = createAiRecommendationMessage(102L);
        givenOwnedChatRoom();
        given(chatMessageRepository.findByIdAndChatRoomId(101L, 123L))
                .willReturn(Optional.of(userMessage));
        given(chatMessageRepository
                .findFirstByChatRoomIdAndIdGreaterThanOrderByIdAsc(
                        123L,
                        101L
                )).willReturn(Optional.of(aiMessage));
        given(recommendationRepository.findAllByMessageIdIn(List.of(102L)))
                .willReturn(List.of());

        assertInvalidServerState(() ->
                chatService.getGenerationStatus(1L, 123L, 101L)
        );
    }

    private void givenOwnedChatRoom() {
        given(chatRoomRepository.findActiveById(123L))
                .willReturn(Optional.of(chatRoom));
    }

    private void verifyNoRecommendationInteractions() {
        verifyNoInteractions(
                recommendationRepository,
                recommendationProductRepository,
                wishlistRepository,
                fittingCandidateRepository
        );
    }

    private void assertErrorCode(
            ThrowingOperation operation,
            ErrorCode expectedErrorCode
    ) {
        assertThatThrownBy(operation::execute)
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(expectedErrorCode)
                );
    }

    private void assertInvalidServerState(ThrowingOperation operation) {
        assertThatThrownBy(operation::execute)
                .isInstanceOf(IllegalStateException.class);
    }

    private Member createMember(Long id) {
        Member createdMember = Member.create(
                "member" + id + "@lookddak.com",
                "encoded-password"
        );
        ReflectionTestUtils.setField(createdMember, "id", id);
        return createdMember;
    }

    private ChatRoom createChatRoom(Long id, Member owner) {
        ChatRoom createdChatRoom = ChatRoom.create(
                owner,
                "가을 출근용 니트 추천해줘",
                ChatSourceType.GENERAL,
                LocalDateTime.now()
        );
        ReflectionTestUtils.setField(createdChatRoom, "id", id);
        return createdChatRoom;
    }

    private ChatMessage createUserMessage(
            Long id,
            ChatGenerationStatus generationStatus
    ) {
        ChatMessage message = ChatMessage.createUserText(chatRoom, "사용자 요청");
        if (generationStatus == ChatGenerationStatus.COMPLETED) {
            message.completeGeneration();
        } else if (generationStatus == ChatGenerationStatus.FAILED) {
            message.failGeneration();
        }
        setMessageFields(message, id);
        return message;
    }

    private ChatMessage createAiTextMessage(Long id) {
        ChatMessage message = ChatMessage.createAiText(
                chatRoom,
                "최종 AI 텍스트 응답"
        );
        setMessageFields(message, id);
        return message;
    }

    private ChatMessage createAiRecommendationMessage(Long id) {
        ChatMessage message = ChatMessage.createAiRecommendation(
                chatRoom,
                "추천 상품을 확인해보세요."
        );
        setMessageFields(message, id);
        return message;
    }

    private Product createProduct(Long id) {
        Product product = Product.create(
                "추천 니트",
                "https://image.lookddak.com/products/201.jpg",
                59_000,
                "BLACK",
                ProductItemType.TOP,
                "https://shop.lookddak.com/products/201"
        );
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    private void setMessageFields(ChatMessage message, Long id) {
        ReflectionTestUtils.setField(message, "id", id);
        ReflectionTestUtils.setField(
                message,
                "createdAt",
                LocalDateTime.of(2026, 9, 22, 12, 30)
        );
    }

    @FunctionalInterface
    private interface ThrowingOperation {

        void execute();
    }
}
