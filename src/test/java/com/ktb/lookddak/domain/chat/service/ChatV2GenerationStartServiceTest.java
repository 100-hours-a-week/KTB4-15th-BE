package com.ktb.lookddak.domain.chat.service;

import com.ktb.lookddak.domain.chat.entity.ChatGenerationStatus;
import com.ktb.lookddak.domain.chat.entity.ChatMessage;
import com.ktb.lookddak.domain.chat.entity.ChatRoom;
import com.ktb.lookddak.domain.chat.entity.ChatSenderType;
import com.ktb.lookddak.domain.chat.entity.ChatSourceType;
import com.ktb.lookddak.domain.chat.event.ChatV2GenerationRequestedEvent;
import com.ktb.lookddak.domain.chat.repository.ChatMessageRepository;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.wishlist.repository.WishlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatV2GenerationStartServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private WishlistRepository wishlistRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ChatV2GenerationStartService service;

    @BeforeEach
    void setUp() {
        service = new ChatV2GenerationStartService(
                chatMessageRepository,
                wishlistRepository,
                eventPublisher
        );
    }

    @Test
    @DisplayName("대기 중인 V2 사용자 메시지만 생성 중으로 변경하고 생성 이벤트를 발행한다")
    void startPendingGeneration() {
        ChatMessage message = createPendingMessage();
        given(chatMessageRepository.findByIdWithChatRoomAndMember(100L))
                .willReturn(Optional.of(message));
        given(chatMessageRepository.updateGenerationStatusIfCurrent(
                100L,
                ChatSenderType.USER,
                ChatGenerationStatus.PENDING,
                ChatGenerationStatus.GENERATING
        )).willReturn(1);

        service.startIfPending(100L);

        ArgumentCaptor<ChatV2GenerationRequestedEvent> captor = ArgumentCaptor
                .forClass(ChatV2GenerationRequestedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getMemberId()).isEqualTo(1L);
        assertThat(captor.getValue().getChatRoomId()).isEqualTo(10L);
        assertThat(captor.getValue().getUserMessageId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("이미 생성 중인 메시지는 AI 생성 이벤트를 중복 발행하지 않는다")
    void doNotStartGenerationTwice() {
        ChatMessage message = createPendingMessage();
        given(chatMessageRepository.findByIdWithChatRoomAndMember(100L))
                .willReturn(Optional.of(message));
        given(chatMessageRepository.updateGenerationStatusIfCurrent(
                any(),
                eq(ChatSenderType.USER),
                eq(ChatGenerationStatus.PENDING),
                eq(ChatGenerationStatus.GENERATING)
        )).willReturn(0);

        service.startIfPending(100L);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("찜 기반 채팅방의 첫 사용자 메시지는 찜 상품 코드와 함께 AI에 요청한다")
    void startFirstWishlistGeneration() {
        ChatMessage message = createPendingMessage(ChatSourceType.WISHLIST, 100L);
        given(chatMessageRepository.findByIdWithChatRoomAndMember(100L))
                .willReturn(Optional.of(message));
        given(chatMessageRepository.updateGenerationStatusIfCurrent(
                100L,
                ChatSenderType.USER,
                ChatGenerationStatus.PENDING,
                ChatGenerationStatus.GENERATING
        )).willReturn(1);
        given(chatMessageRepository.existsByChatRoomIdAndSenderTypeAndIdLessThan(
                10L,
                ChatSenderType.USER,
                100L
        )).willReturn(false);
        given(wishlistRepository.findRecentProductCodesByMemberId(
                eq(1L),
                any()
        )).willReturn(List.of("0000001", "0000002"));

        service.startIfPending(100L);

        ArgumentCaptor<ChatV2GenerationRequestedEvent> captor = ArgumentCaptor
                .forClass(ChatV2GenerationRequestedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getSourceType())
                .isEqualTo(ChatSourceType.WISHLIST);
        assertThat(captor.getValue().getProductIds())
                .containsExactly("0000001", "0000002");
    }

    @Test
    @DisplayName("찜 기반 채팅방의 두 번째 사용자 메시지는 일반 대화로 AI에 요청한다")
    void startFollowingWishlistMessageAsGeneralGeneration() {
        ChatMessage message = createPendingMessage(ChatSourceType.WISHLIST, 101L);
        given(chatMessageRepository.findByIdWithChatRoomAndMember(101L))
                .willReturn(Optional.of(message));
        given(chatMessageRepository.updateGenerationStatusIfCurrent(
                101L,
                ChatSenderType.USER,
                ChatGenerationStatus.PENDING,
                ChatGenerationStatus.GENERATING
        )).willReturn(1);
        given(chatMessageRepository.existsByChatRoomIdAndSenderTypeAndIdLessThan(
                10L,
                ChatSenderType.USER,
                101L
        )).willReturn(true);

        service.startIfPending(101L);

        ArgumentCaptor<ChatV2GenerationRequestedEvent> captor = ArgumentCaptor
                .forClass(ChatV2GenerationRequestedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getSourceType())
                .isEqualTo(ChatSourceType.GENERAL);
        assertThat(captor.getValue().getProductIds()).isEmpty();
        verify(wishlistRepository, never()).findRecentProductCodesByMemberId(
                any(),
                any()
        );
    }

    private ChatMessage createPendingMessage() {
        return createPendingMessage(ChatSourceType.GENERAL, 100L);
    }

    private ChatMessage createPendingMessage(
            ChatSourceType sourceType,
            Long messageId
    ) {
        Member member = Member.create("member@lookddak.com", "encoded-password");
        ReflectionTestUtils.setField(member, "id", 1L);
        ChatRoom chatRoom = ChatRoom.create(
                member,
                "가을 니트를 추천해줘",
                sourceType,
                LocalDateTime.now()
        );
        ReflectionTestUtils.setField(chatRoom, "id", 10L);
        ChatMessage message = ChatMessage.createPendingUserText(
                chatRoom,
                "가을 니트를 추천해줘"
        );
        ReflectionTestUtils.setField(message, "id", messageId);
        return message;
    }
}
