package com.ktb.lookddak.domain.chat.service;

import com.ktb.lookddak.domain.chat.dto.ChatMessageCreateRequest;
import com.ktb.lookddak.domain.chat.dto.ChatMessageCreateResponse;
import com.ktb.lookddak.domain.chat.dto.ChatRoomCreateRequest;
import com.ktb.lookddak.domain.chat.dto.ChatRoomCreateResponse;
import com.ktb.lookddak.domain.chat.dto.ChatRoomListResponse;
import com.ktb.lookddak.domain.chat.dto.ChatRoomTitleUpdateRequest;
import com.ktb.lookddak.domain.chat.dto.ChatRoomTitleUpdateResponse;
import com.ktb.lookddak.domain.chat.entity.ChatGenerationStatus;
import com.ktb.lookddak.domain.chat.entity.ChatMessage;
import com.ktb.lookddak.domain.chat.entity.ChatMessageType;
import com.ktb.lookddak.domain.chat.entity.ChatRoom;
import com.ktb.lookddak.domain.chat.entity.ChatSenderType;
import com.ktb.lookddak.domain.chat.entity.ChatSourceType;
import com.ktb.lookddak.domain.chat.repository.ChatMessageRepository;
import com.ktb.lookddak.domain.chat.repository.ChatRoomRepository;
import com.ktb.lookddak.domain.fitting.repository.FittingCandidateRepository;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.member.repository.MemberRepository;
import com.ktb.lookddak.domain.recommendation.repository.RecommendationProductRepository;
import com.ktb.lookddak.domain.recommendation.repository.RecommendationRepository;
import com.ktb.lookddak.domain.wishlist.repository.WishlistRepository;
import com.ktb.lookddak.global.exception.BusinessException;
import com.ktb.lookddak.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

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
    }

    @Test
    @DisplayName("새 채팅방과 첫 사용자 메시지를 생성한다")
    void createChatRoom() {
        Member member = createMember(1L);
        LocalDateTime createdAt = LocalDateTime.of(
                2026, 9, 24, 16, 50
        );
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
                    ReflectionTestUtils.setField(
                            message,
                            "createdAt",
                            createdAt
                    );
                    return message;
                });

        ChatRoomCreateResponse response = chatService.createChatRoom(1L, request);

        assertThat(response.getChatRoomId()).isEqualTo(10L);
        assertThat(response.getMessageId()).isEqualTo(100L);
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);

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
        LocalDateTime createdAt = LocalDateTime.of(
                2026, 9, 24, 16, 55
        );
        LocalDateTime initialLastMessageAt = LocalDateTime.now().minusMinutes(10);
        ChatRoom chatRoom = createChatRoom(10L, member, initialLastMessageAt);
        ChatMessageCreateRequest request = new ChatMessageCreateRequest("검은색으로 추천해줘");
        given(chatRoomRepository.findActiveByIdForUpdate(10L))
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
                    ReflectionTestUtils.setField(
                            message,
                            "createdAt",
                            createdAt
                    );
                    return message;
                });

        ChatMessageCreateResponse response = chatService.createMessage(1L, 10L, request);

        assertThat(response.getChatRoomId()).isEqualTo(10L);
        assertThat(response.getMessageId()).isEqualTo(101L);
        assertThat(response.getContent()).isEqualTo("검은색으로 추천해줘");
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
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
        given(chatRoomRepository.findActiveByIdForUpdate(10L))
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
        given(chatRoomRepository.findActiveByIdForUpdate(10L))
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
        given(chatRoomRepository.findActiveByIdForUpdate(10L))
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

    @Test
    @DisplayName("소유한 채팅방의 제목을 수정한다")
    void updateTitle() {
        Member member = createMember(1L);
        ChatRoom chatRoom = createChatRoom(10L, member, LocalDateTime.now());
        ChatRoomTitleUpdateRequest request =
                new ChatRoomTitleUpdateRequest("  가을 출근용 니트 추천  ");
        given(chatRoomRepository.findActiveByIdForUpdate(10L))
                .willReturn(Optional.of(chatRoom));

        ChatRoomTitleUpdateResponse response =
                chatService.updateTitle(1L, 10L, request);

        assertThat(response.getChatRoomId()).isEqualTo(10L);
        assertThat(response.getTitle()).isEqualTo("가을 출근용 니트 추천");
        assertThat(chatRoom.getTitle()).isEqualTo("가을 출근용 니트 추천");
    }

    @Test
    @DisplayName("존재하지 않거나 삭제된 채팅방의 제목은 수정할 수 없다")
    void rejectTitleUpdateForMissingChatRoom() {
        ChatRoomTitleUpdateRequest request =
                new ChatRoomTitleUpdateRequest("가을 출근용 니트 추천");
        given(chatRoomRepository.findActiveByIdForUpdate(10L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.updateTitle(1L, 10L, request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.CHAT_ROOM_NOT_FOUND)
                );
    }

    @Test
    @DisplayName("다른 회원의 채팅방 제목은 수정할 수 없다")
    void rejectOtherMembersTitleUpdate() {
        Member owner = createMember(2L);
        ChatRoom chatRoom = createChatRoom(10L, owner, LocalDateTime.now());
        ChatRoomTitleUpdateRequest request =
                new ChatRoomTitleUpdateRequest("가을 출근용 니트 추천");
        given(chatRoomRepository.findActiveByIdForUpdate(10L))
                .willReturn(Optional.of(chatRoom));

        assertThatThrownBy(() -> chatService.updateTitle(1L, 10L, request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.CHAT_ROOM_ACCESS_DENIED)
                );
    }

    @Test
    @DisplayName("소유한 채팅방을 소프트 삭제한다")
    void deleteChatRoom() {
        Member member = createMember(1L);
        ChatRoom chatRoom = createChatRoom(10L, member, LocalDateTime.now());
        given(chatRoomRepository.findActiveByIdForUpdate(10L))
                .willReturn(Optional.of(chatRoom));

        chatService.deleteChatRoom(1L, 10L);

        assertThat(chatRoom.isDeleted()).isTrue();
        assertThat(chatRoom.getDeletedAt()).isNotNull();
        verify(chatRoomRepository, never()).delete(any(ChatRoom.class));
    }

    @Test
    @DisplayName("존재하지 않거나 이미 삭제된 채팅방은 삭제할 수 없다")
    void rejectMissingChatRoomDeletion() {
        given(chatRoomRepository.findActiveByIdForUpdate(10L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.deleteChatRoom(1L, 10L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.CHAT_ROOM_NOT_FOUND)
                );

        verify(chatRoomRepository, never()).delete(any(ChatRoom.class));
    }

    @Test
    @DisplayName("다른 회원의 채팅방은 삭제할 수 없다")
    void rejectOtherMembersChatRoomDeletion() {
        Member owner = createMember(2L);
        ChatRoom chatRoom = createChatRoom(10L, owner, LocalDateTime.now());
        given(chatRoomRepository.findActiveByIdForUpdate(10L))
                .willReturn(Optional.of(chatRoom));

        assertThatThrownBy(() -> chatService.deleteChatRoom(1L, 10L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.CHAT_ROOM_ACCESS_DENIED)
                );

        assertThat(chatRoom.isDeleted()).isFalse();
        verify(chatRoomRepository, never()).delete(any(ChatRoom.class));
    }

    @Test
    @DisplayName("첫 페이지에 다음 데이터가 있으면 nextCursor와 hasNext를 반환한다")
    void getFirstChatRoomPage() {
        Member member = createMember(1L);
        ChatRoom firstRoom = createChatRoom(
                30L,
                member,
                LocalDateTime.of(2026, 9, 21, 15, 0)
        );
        ChatRoom secondRoom = createChatRoom(
                20L,
                member,
                LocalDateTime.of(2026, 9, 21, 14, 0)
        );
        ChatRoom nextPageRoom = createChatRoom(
                10L,
                member,
                LocalDateTime.of(2026, 9, 21, 13, 0)
        );
        given(chatRoomRepository.findFirstPage(eq(1L), any(Pageable.class)))
                .willReturn(List.of(firstRoom, secondRoom, nextPageRoom));

        ChatRoomListResponse response = chatService.getChatRooms(1L, null, 2);

        assertThat(response.getItems())
                .extracting(item -> item.getChatRoomId())
                .containsExactly(30L, 20L);
        assertThat(response.getNextCursor()).isEqualTo(20L);
        assertThat(response.isHasNext()).isTrue();

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);
        verify(chatRoomRepository).findFirstPage(eq(1L), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(3);
    }

    @Test
    @DisplayName("마지막 페이지는 null Cursor와 hasNext false를 반환한다")
    void getLastChatRoomPage() {
        Member member = createMember(1L);
        ChatRoom chatRoom = createChatRoom(
                10L,
                member,
                LocalDateTime.of(2026, 9, 21, 13, 0)
        );
        given(chatRoomRepository.findFirstPage(eq(1L), any(Pageable.class)))
                .willReturn(List.of(chatRoom));

        ChatRoomListResponse response = chatService.getChatRooms(1L, null, 2);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getNextCursor()).isNull();
        assertThat(response.isHasNext()).isFalse();
    }

    @Test
    @DisplayName("채팅방이 없으면 기본 크기로 빈 목록을 조회한다")
    void getEmptyChatRoomPageWithDefaultSize() {
        given(chatRoomRepository.findFirstPage(eq(1L), any(Pageable.class)))
                .willReturn(List.of());

        ChatRoomListResponse response = chatService.getChatRooms(1L, null, null);

        assertThat(response.getItems()).isEmpty();
        assertThat(response.getNextCursor()).isNull();
        assertThat(response.isHasNext()).isFalse();

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);
        verify(chatRoomRepository).findFirstPage(eq(1L), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(21);
    }

    @Test
    @DisplayName("유효한 Cursor 채팅방을 기준으로 다음 페이지를 조회한다")
    void getNextChatRoomPage() {
        Member member = createMember(1L);
        LocalDateTime cursorTime = LocalDateTime.of(2026, 9, 21, 14, 0);
        ChatRoom cursorRoom = createChatRoom(25L, member, cursorTime);
        ChatRoom nextRoom = createChatRoom(
                18L,
                member,
                cursorTime.minusHours(1)
        );
        given(chatRoomRepository.findActiveCursor(1L, 25L))
                .willReturn(Optional.of(cursorRoom));
        given(chatRoomRepository.findNextPage(
                eq(1L),
                eq(cursorTime),
                eq(25L),
                any(Pageable.class)
        )).willReturn(List.of(nextRoom));

        ChatRoomListResponse response = chatService.getChatRooms(1L, 25L, 20);

        assertThat(response.getItems())
                .extracting(item -> item.getChatRoomId())
                .containsExactly(18L);
        assertThat(response.getNextCursor()).isNull();
        assertThat(response.isHasNext()).isFalse();
    }

    @Test
    @DisplayName("Cursor나 조회 크기가 범위를 벗어나면 목록 조회를 거부한다")
    void rejectInvalidPaginationRange() {
        assertPaginationError(() -> chatService.getChatRooms(1L, 0L, 20));
        assertPaginationError(() -> chatService.getChatRooms(1L, -1L, 20));
        assertPaginationError(() -> chatService.getChatRooms(1L, null, 0));
        assertPaginationError(() -> chatService.getChatRooms(1L, null, 101));

        verifyNoInteractions(chatRoomRepository);
    }

    @Test
    @DisplayName("회원의 활성 채팅방이 아닌 Cursor는 목록 조회에 사용할 수 없다")
    void rejectInvalidCursor() {
        given(chatRoomRepository.findActiveCursor(1L, 25L))
                .willReturn(Optional.empty());

        assertPaginationError(() -> chatService.getChatRooms(1L, 25L, 20));

        verify(chatRoomRepository, never()).findNextPage(
                any(),
                any(),
                any(),
                any()
        );
    }

    private void assertPaginationError(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
        assertThatThrownBy(callable)
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_PAGINATION_PARAMETER)
                );
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
