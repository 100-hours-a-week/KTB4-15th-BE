package com.ktb.lookddak.domain.chat.service;

import com.ktb.lookddak.domain.chat.dto.ChatMessageCreateRequest;
import com.ktb.lookddak.domain.chat.dto.ChatMessageCreateResponse;
import com.ktb.lookddak.domain.chat.dto.ChatRoomCreateRequest;
import com.ktb.lookddak.domain.chat.dto.ChatRoomCreateResponse;
import com.ktb.lookddak.domain.chat.dto.ChatRoomListItemResponse;
import com.ktb.lookddak.domain.chat.dto.ChatRoomListResponse;
import com.ktb.lookddak.domain.chat.dto.ChatRoomTitleUpdateRequest;
import com.ktb.lookddak.domain.chat.dto.ChatRoomTitleUpdateResponse;
import com.ktb.lookddak.domain.chat.entity.ChatGenerationStatus;
import com.ktb.lookddak.domain.chat.entity.ChatMessage;
import com.ktb.lookddak.domain.chat.entity.ChatRoom;
import com.ktb.lookddak.domain.chat.entity.ChatSenderType;
import com.ktb.lookddak.domain.chat.repository.ChatMessageRepository;
import com.ktb.lookddak.domain.chat.repository.ChatRoomRepository;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.member.repository.MemberRepository;
import com.ktb.lookddak.global.exception.BusinessException;
import com.ktb.lookddak.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 100;

    private final MemberRepository memberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Transactional
    public ChatRoomCreateResponse createChatRoom(
            Long memberId,
            ChatRoomCreateRequest request
    ) {
        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        LocalDateTime messageCreatedAt = LocalDateTime.now();
        ChatRoom chatRoom = ChatRoom.create(
                member,
                request.getContent(),
                request.getSourceType(),
                messageCreatedAt
        );
        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);

        ChatMessage message = ChatMessage.createUserText(
                savedChatRoom,
                request.getContent()
        );
        ChatMessage savedMessage = chatMessageRepository.save(message);

        return new ChatRoomCreateResponse(
                savedChatRoom.getId(),
                savedMessage.getId()
        );
    }

    @Transactional
    public ChatMessageCreateResponse createMessage(
            Long memberId,
            Long chatRoomId,
            ChatMessageCreateRequest request
    ) {
        ChatRoom chatRoom = getOwnedActiveChatRoom(memberId, chatRoomId);

        boolean isGenerating = chatMessageRepository
                .existsByChatRoomIdAndSenderTypeAndGenerationStatus(
                        chatRoomId,
                        ChatSenderType.USER,
                        ChatGenerationStatus.GENERATING
                );

        if (isGenerating) {
            throw new BusinessException(ErrorCode.AI_RESPONSE_GENERATING);
        }

        ChatMessage message = ChatMessage.createUserText(
                chatRoom,
                request.getContent()
        );
        ChatMessage savedMessage = chatMessageRepository.save(message);

        chatRoom.updateLastMessageAt(LocalDateTime.now());

        return new ChatMessageCreateResponse(
                chatRoomId,
                savedMessage.getId(),
                savedMessage.getContent()
        );
    }

    @Transactional
    public ChatRoomTitleUpdateResponse updateTitle(
            Long memberId,
            Long chatRoomId,
            ChatRoomTitleUpdateRequest request
    ) {
        ChatRoom chatRoom = getOwnedActiveChatRoom(memberId, chatRoomId);

        chatRoom.updateTitle(request.getTitle());

        return new ChatRoomTitleUpdateResponse(
                chatRoom.getId(),
                chatRoom.getTitle()
        );
    }

    @Transactional
    public void deleteChatRoom(Long memberId, Long chatRoomId) {
        ChatRoom chatRoom = getOwnedActiveChatRoom(memberId, chatRoomId);

        chatRoom.delete(LocalDateTime.now());
    }

    public ChatRoomListResponse getChatRooms(
            Long memberId,
            Long cursor,
            Integer size
    ) {
        int pageSize = size == null ? DEFAULT_PAGE_SIZE : size;
        validatePagination(cursor, pageSize);

        PageRequest pageRequest = PageRequest.of(0, pageSize + 1);
        List<ChatRoom> chatRooms;

        if (cursor == null) {
            chatRooms = chatRoomRepository.findFirstPage(memberId, pageRequest);
        } else {
            ChatRoom cursorRoom = chatRoomRepository
                    .findActiveCursor(memberId, cursor)
                    .orElseThrow(() ->
                            new BusinessException(
                                    ErrorCode.INVALID_PAGINATION_PARAMETER
                            )
                    );

            chatRooms = chatRoomRepository.findNextPage(
                    memberId,
                    cursorRoom.getLastMessageAt(),
                    cursorRoom.getId(),
                    pageRequest
            );
        }

        boolean hasNext = chatRooms.size() > pageSize;
        List<ChatRoom> responseRooms = hasNext
                ? chatRooms.subList(0, pageSize)
                : chatRooms;

        List<ChatRoomListItemResponse> items = responseRooms.stream()
                .map(ChatRoomListItemResponse::from)
                .toList();

        Long nextCursor = hasNext
                ? responseRooms.get(responseRooms.size() - 1).getId()
                : null;

        return new ChatRoomListResponse(items, nextCursor, hasNext);
    }

    private void validatePagination(Long cursor, int size) {
        if ((cursor != null && cursor <= 0)
                || size < MIN_PAGE_SIZE
                || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_PAGINATION_PARAMETER);
        }
    }

    private ChatRoom getOwnedActiveChatRoom(Long memberId, Long chatRoomId) {
        ChatRoom chatRoom = chatRoomRepository.findActiveByIdForUpdate(chatRoomId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND)
                );

        if (!chatRoom.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }

        return chatRoom;
    }
}
