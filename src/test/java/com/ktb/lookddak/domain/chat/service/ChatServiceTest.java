package com.ktb.lookddak.domain.chat.service;

import com.ktb.lookddak.domain.chat.dto.ChatMessageCreateRequest;
import com.ktb.lookddak.domain.chat.dto.ChatMessageCreateResponse;
import com.ktb.lookddak.domain.chat.dto.ChatRoomCreateRequest;
import com.ktb.lookddak.domain.chat.dto.ChatRoomCreateResponse;
import com.ktb.lookddak.domain.chat.entity.ChatGenerationStatus;
import com.ktb.lookddak.domain.chat.entity.ChatMessage;
import com.ktb.lookddak.domain.chat.entity.ChatMessageType;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(
                memberRepository,
                chatRoomRepository,
                chatMessageRepository
        );
    }

    @Test
    @DisplayName("새 채팅방과 첫 사용자 메시지를 생성한다")
    void createChatRoom() {
        Member member = createMember(1L);
        ChatRoomCreateRequest request = new ChatRoomCreateRequest(
                "5만원대 캐주얼 니트 추천해줘",
                ChatSourceType.WISHLIST
        );
        given(memberRepository.findByIdAndDeletedAtIsNull(1L))
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
                    return message;
                });

        ChatRoomCreateResponse response = chatService.createChatRoom(1L, request);

        assertThat(response.getChatRoomId()).isEqualTo(10L);
        assertThat(response.getMessageId()).isEqualTo(100L);

        ArgumentCaptor<ChatRoom> chatRoomCaptor = ArgumentCaptor.forClass(ChatRoom.class);
        verify(chatRoomRepository).save(chatRoomCaptor.capture());
        ChatRoom savedChatRoom = chatRoomCaptor.getValue();
        assertThat(savedChatRoom.getMember()).isSameAs(member);
        assertThat(savedChatRoom.getTitle()).isEqualTo("5만원대 캐주얼 니트 추천해줘");
        assertThat(savedChatRoom.getSourceType()).isEqualTo(ChatSourceType.WISHLIST);

        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(messageCaptor.capture());
        ChatMessage savedMessage = messageCaptor.getValue();
        assertThat(savedMessage.getChatRoom()).isSameAs(savedChatRoom);
        assertThat(savedMessage.getSenderType()).isEqualTo(ChatSenderType.USER);
        assertThat(savedMessage.getMessageType()).isEqualTo(ChatMessageType.TEXT);
        assertThat(savedMessage.getGenerationStatus())
                .isEqualTo(ChatGenerationStatus.GENERATING);
        assertThat(savedMessage.getContent()).isEqualTo(request.getContent());
    }

    @Test
    @DisplayName("활성 회원이 없으면 새 대화를 시작할 수 없다")
    void rejectMissingMember() {
        ChatRoomCreateRequest request = new ChatRoomCreateRequest(
                "옷을 추천해줘",
                ChatSourceType.GENERAL
        );
        given(memberRepository.findByIdAndDeletedAtIsNull(1L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.createChatRoom(1L, request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND)
                );

        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("기존 채팅방에 새로운 사용자 메시지를 저장한다")
    void createMessage() {
        Member member = createMember(1L);
        LocalDateTime initialLastMessageAt = LocalDateTime.now().minusMinutes(10);
        ChatRoom chatRoom = createChatRoom(10L, member, initialLastMessageAt);
        ChatMessageCreateRequest request = new ChatMessageCreateRequest("검은색으로 추천해줘");
        given(chatRoomRepository.findByIdForUpdate(10L))
                .willReturn(Optional.of(chatRoom));
        given(chatMessageRepository
                .existsByChatRoomIdAndSenderTypeAndGenerationStatus(
                        10L,
                        ChatSenderType.USER,
                        ChatGenerationStatus.GENERATING
                ))
                .willReturn(false);
        given(chatMessageRepository.save(any(ChatMessage.class)))
                .willAnswer(invocation -> {
                    ChatMessage message = invocation.getArgument(0);
                    ReflectionTestUtils.setField(message, "id", 101L);
                    return message;
                });

        ChatMessageCreateResponse response = chatService.createMessage(1L, 10L, request);

        assertThat(response.getChatRoomId()).isEqualTo(10L);
        assertThat(response.getMessageId()).isEqualTo(101L);
        assertThat(response.getContent()).isEqualTo("검은색으로 추천해줘");
        assertThat(chatRoom.getLastMessageAt()).isAfter(initialLastMessageAt);

        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(messageCaptor.capture());
        ChatMessage savedMessage = messageCaptor.getValue();
        assertThat(savedMessage.getSenderType()).isEqualTo(ChatSenderType.USER);
        assertThat(savedMessage.getMessageType()).isEqualTo(ChatMessageType.TEXT);
        assertThat(savedMessage.getGenerationStatus())
                .isEqualTo(ChatGenerationStatus.GENERATING);
    }

    @Test
    @DisplayName("채팅방이 없으면 메시지를 전송할 수 없다")
    void rejectMissingChatRoom() {
        ChatMessageCreateRequest request = new ChatMessageCreateRequest("검은색으로 추천해줘");
        given(chatRoomRepository.findByIdForUpdate(10L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.createMessage(1L, 10L, request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.CHAT_ROOM_NOT_FOUND)
                );

        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("다른 회원의 채팅방에는 메시지를 전송할 수 없다")
    void rejectOtherMembersChatRoom() {
        Member owner = createMember(2L);
        ChatRoom chatRoom = createChatRoom(10L, owner, LocalDateTime.now());
        ChatMessageCreateRequest request = new ChatMessageCreateRequest("검은색으로 추천해줘");
        given(chatRoomRepository.findByIdForUpdate(10L))
                .willReturn(Optional.of(chatRoom));

        assertThatThrownBy(() -> chatService.createMessage(1L, 10L, request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.CHAT_ROOM_ACCESS_DENIED)
                );

        verify(chatMessageRepository, never())
                .existsByChatRoomIdAndSenderTypeAndGenerationStatus(
                        any(),
                        any(),
                        any()
                );
        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("AI 응답 생성 중에는 새로운 메시지를 전송할 수 없다")
    void rejectWhileGeneratingResponse() {
        Member member = createMember(1L);
        ChatRoom chatRoom = createChatRoom(10L, member, LocalDateTime.now());
        ChatMessageCreateRequest request = new ChatMessageCreateRequest("검은색으로 추천해줘");
        given(chatRoomRepository.findByIdForUpdate(10L))
                .willReturn(Optional.of(chatRoom));
        given(chatMessageRepository
                .existsByChatRoomIdAndSenderTypeAndGenerationStatus(
                        10L,
                        ChatSenderType.USER,
                        ChatGenerationStatus.GENERATING
                ))
                .willReturn(true);

        assertThatThrownBy(() -> chatService.createMessage(1L, 10L, request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.AI_RESPONSE_GENERATING)
                );

        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    }

    private Member createMember(Long memberId) {
        Member member = Member.create("member" + memberId + "@lookddak.com", "encoded-password");
        ReflectionTestUtils.setField(member, "id", memberId);
        return member;
    }

    private ChatRoom createChatRoom(
            Long chatRoomId,
            Member member,
            LocalDateTime lastMessageAt
    ) {
        ChatRoom chatRoom = ChatRoom.create(
                member,
                "5만원대 캐주얼 니트 추천해줘",
                ChatSourceType.GENERAL,
                lastMessageAt
        );
        ReflectionTestUtils.setField(chatRoom, "id", chatRoomId);
        return chatRoom;
    }
}
