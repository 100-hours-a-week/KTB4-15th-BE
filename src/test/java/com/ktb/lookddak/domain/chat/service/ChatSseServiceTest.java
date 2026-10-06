package com.ktb.lookddak.domain.chat.service;

import com.ktb.lookddak.domain.chat.entity.ChatMessage;
import com.ktb.lookddak.domain.chat.entity.ChatRoom;
import com.ktb.lookddak.domain.chat.entity.ChatSourceType;
import com.ktb.lookddak.domain.chat.repository.ChatMessageRepository;
import com.ktb.lookddak.domain.chat.repository.ChatRoomRepository;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.global.exception.BusinessException;
import com.ktb.lookddak.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatSseServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Test
    @DisplayName("채팅방 소유자는 사용자 메시지 SSE 연결을 생성할 수 있다")
    void connect() {
        ChatSseService service = new ChatSseService(
                chatRoomRepository,
                chatMessageRepository
        );
        Member member = createMember(1L);
        ChatRoom chatRoom = createChatRoom(10L, member);
        ChatMessage message = ChatMessage.createPendingUserText(
                chatRoom,
                "가을 니트를 추천해줘"
        );
        ReflectionTestUtils.setField(message, "id", 100L);
        given(chatRoomRepository.findActiveById(10L))
                .willReturn(Optional.of(chatRoom));
        given(chatMessageRepository.findByIdAndChatRoomId(100L, 10L))
                .willReturn(Optional.of(message));

        SseEmitter emitter = service.connect(1L, 10L, 100L);

        assertThat(emitter.getTimeout()).isEqualTo(300_000L);
        assertThat(service.hasConnection(100L)).isTrue();
    }

    @Test
    @DisplayName("다른 회원의 채팅방에는 SSE 연결할 수 없다")
    void rejectWhenChatRoomIsNotOwned() {
        ChatSseService service = new ChatSseService(
                chatRoomRepository,
                chatMessageRepository
        );
        ChatRoom chatRoom = createChatRoom(10L, createMember(2L));
        given(chatRoomRepository.findActiveById(10L))
                .willReturn(Optional.of(chatRoom));

        assertThatThrownBy(() -> service.connect(1L, 10L, 100L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.CHAT_ROOM_ACCESS_DENIED)
                );
    }

    @Test
    @DisplayName("채팅방에 속하지 않는 메시지에는 SSE 연결할 수 없다")
    void rejectWhenMessageDoesNotExist() {
        ChatSseService service = new ChatSseService(
                chatRoomRepository,
                chatMessageRepository
        );
        ChatRoom chatRoom = createChatRoom(10L, createMember(1L));
        given(chatRoomRepository.findActiveById(10L))
                .willReturn(Optional.of(chatRoom));
        given(chatMessageRepository.findByIdAndChatRoomId(100L, 10L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.connect(1L, 10L, 100L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.CHAT_MESSAGE_NOT_FOUND)
                );
        verify(chatMessageRepository).findByIdAndChatRoomId(100L, 10L);
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
