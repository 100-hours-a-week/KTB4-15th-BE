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
        given(chatMessageRepository.findById(100L))
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
        given(chatMessageRepository.findById(100L))
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

    private ChatMessage createPendingMessage() {
        Member member = Member.create("member@lookddak.com", "encoded-password");
        ReflectionTestUtils.setField(member, "id", 1L);
        ChatRoom chatRoom = ChatRoom.create(
                member,
                "가을 니트를 추천해줘",
                ChatSourceType.GENERAL,
                LocalDateTime.now()
        );
        ReflectionTestUtils.setField(chatRoom, "id", 10L);
        ChatMessage message = ChatMessage.createPendingUserText(
                chatRoom,
                "가을 니트를 추천해줘"
        );
        ReflectionTestUtils.setField(message, "id", 100L);
        return message;
    }
}
