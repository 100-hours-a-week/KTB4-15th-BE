package com.ktb.lookddak.domain.chat.service;

import com.ktb.lookddak.domain.chat.dto.ChatMessageCreateRequest;
import com.ktb.lookddak.domain.chat.dto.ChatMessageCreateResponse;
import com.ktb.lookddak.domain.chat.dto.ChatRoomCreateRequest;
import com.ktb.lookddak.domain.chat.dto.ChatRoomCreateResponse;
import com.ktb.lookddak.domain.chat.entity.ChatGenerationStatus;
import com.ktb.lookddak.domain.chat.entity.ChatMessage;
import com.ktb.lookddak.domain.chat.entity.ChatRoom;
import com.ktb.lookddak.domain.chat.entity.ChatSenderType;
import com.ktb.lookddak.domain.chat.entity.ChatSourceType;
import com.ktb.lookddak.domain.chat.repository.ChatMessageRepository;
import com.ktb.lookddak.domain.chat.repository.ChatRoomRepository;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.member.repository.MemberRepository;
import com.ktb.lookddak.global.exception.BusinessException;
import com.ktb.lookddak.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatV2ServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    private ChatV2Service chatV2Service;

    @BeforeEach
    void setUp() {
        chatV2Service = new ChatV2Service(
                memberRepository,
                chatRoomRepository,
                chatMessageRepository
        );
    }

    @Test
    @DisplayName("V2 새 대화는 첫 사용자 메시지를 대기 상태로 저장한다")
    void createChatRoom() {
        Member member = createMember(1L);
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 6, 22, 0);
        given(memberRepository.findActiveByIdForUpdate(1L))
                .willReturn(Optional.of(member));
        given(chatRoomRepository.save(any(ChatRoom.class)))
                .willAnswer(invocation -> {
                    ChatRoom chatRoom = invocation.getArgument(0);
                    ReflectionTestUtils.setField(chatRoom, "id", 10L);
                    return chatRoom;
                });
        given(chatMessageRepository.save(any(ChatMessage.class)))
                .willAnswer(invocation -> {
                    ChatMessage message = invocation.getArgument(0);
                    ReflectionTestUtils.setField(message, "id", 100L);
                    ReflectionTestUtils.setField(message, "createdAt", createdAt);
                    return message;
                });

        ChatRoomCreateResponse response = chatV2Service.createChatRoom(
                1L,
                new ChatRoomCreateRequest(
                        "가을 니트를 추천해줘",
                        ChatSourceType.GENERAL
                )
        );

        assertThat(response.getChatRoomId()).isEqualTo(10L);
        assertThat(response.getMessageId()).isEqualTo(100L);
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor
                .forClass(ChatMessage.class);
        verify(chatMessageRepository).save(captor.capture());
        assertThat(captor.getValue().getGenerationStatus())
                .isEqualTo(ChatGenerationStatus.PENDING);
    }

    @Test
    @DisplayName("V2 메시지 전송은 대기 상태로 저장한다")
    void createMessage() {
        Member member = createMember(1L);
        ChatRoom chatRoom = createChatRoom(10L, member);
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 6, 22, 5);
        given(chatRoomRepository.findActiveByIdForUpdate(10L))
                .willReturn(Optional.of(chatRoom));
        given(chatMessageRepository
                .existsByChatRoomIdAndSenderTypeAndGenerationStatusIn(
                        eq(10L),
                        eq(ChatSenderType.USER),
                        eq(List.of(
                                ChatGenerationStatus.PENDING,
                                ChatGenerationStatus.GENERATING
                        ))
                ))
                .willReturn(false);
        given(chatMessageRepository.save(any(ChatMessage.class)))
                .willAnswer(invocation -> {
                    ChatMessage message = invocation.getArgument(0);
                    ReflectionTestUtils.setField(message, "id", 101L);
                    ReflectionTestUtils.setField(message, "createdAt", createdAt);
                    return message;
                });

        ChatMessageCreateResponse response = chatV2Service.createMessage(
                1L,
                10L,
                new ChatMessageCreateRequest("검은색으로 추천해줘")
        );

        assertThat(response.getMessageId()).isEqualTo(101L);
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor
                .forClass(ChatMessage.class);
        verify(chatMessageRepository).save(captor.capture());
        assertThat(captor.getValue().getGenerationStatus())
                .isEqualTo(ChatGenerationStatus.PENDING);
    }

    @Test
    @DisplayName("대기 또는 생성 중인 메시지가 있으면 V2 메시지를 추가 전송할 수 없다")
    void rejectMessageWhenGenerationIsActive() {
        Member member = createMember(1L);
        ChatRoom chatRoom = createChatRoom(10L, member);
        given(chatRoomRepository.findActiveByIdForUpdate(10L))
                .willReturn(Optional.of(chatRoom));
        given(chatMessageRepository
                .existsByChatRoomIdAndSenderTypeAndGenerationStatusIn(
                        eq(10L),
                        eq(ChatSenderType.USER),
                        eq(List.of(
                                ChatGenerationStatus.PENDING,
                                ChatGenerationStatus.GENERATING
                        ))
                ))
                .willReturn(true);

        assertThatThrownBy(() -> chatV2Service.createMessage(
                1L,
                10L,
                new ChatMessageCreateRequest("다른 색상으로 추천해줘")
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.AI_RESPONSE_GENERATING)
        );
    }

    private Member createMember(Long memberId) {
        Member member = Member.create(
                "member" + memberId + "@lookddak.com",
                "encoded-password"
        );
        ReflectionTestUtils.setField(member, "id", memberId);
        return member;
    }

    private ChatRoom createChatRoom(Long chatRoomId, Member member) {
        ChatRoom chatRoom = ChatRoom.create(
                member,
                "기존 채팅방",
                ChatSourceType.GENERAL,
                LocalDateTime.now().minusMinutes(1)
        );
        ReflectionTestUtils.setField(chatRoom, "id", chatRoomId);
        return chatRoom;
    }
}
