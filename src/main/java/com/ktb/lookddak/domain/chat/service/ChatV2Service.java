package com.ktb.lookddak.domain.chat.service;

import com.ktb.lookddak.domain.chat.dto.ChatMessageCreateRequest;
import com.ktb.lookddak.domain.chat.dto.ChatMessageCreateResponse;
import com.ktb.lookddak.domain.chat.dto.ChatRoomCreateRequest;
import com.ktb.lookddak.domain.chat.dto.ChatRoomCreateResponse;
import com.ktb.lookddak.domain.chat.entity.ChatGenerationStatus;
import com.ktb.lookddak.domain.chat.entity.ChatMessage;
import com.ktb.lookddak.domain.chat.entity.ChatRoom;
import com.ktb.lookddak.domain.chat.entity.ChatSenderType;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.member.repository.MemberRepository;
import com.ktb.lookddak.domain.chat.repository.ChatMessageRepository;
import com.ktb.lookddak.domain.chat.repository.ChatRoomRepository;
import com.ktb.lookddak.global.exception.BusinessException;
import com.ktb.lookddak.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatV2Service {

    private static final long MAX_CHAT_ROOM_COUNT = 100L;
    private static final List<ChatGenerationStatus> ACTIVE_STATUSES = List.of(
            ChatGenerationStatus.PENDING,
            ChatGenerationStatus.GENERATING
    );

    private final MemberRepository memberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Transactional
    public ChatRoomCreateResponse createChatRoom(
            Long memberId,
            ChatRoomCreateRequest request
    ) {
        Member member = memberRepository.findActiveByIdForUpdate(memberId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND
                ));

        long chatRoomCount = chatRoomRepository
                .countByMemberIdAndDeletedAtIsNull(memberId);

        if (chatRoomCount >= MAX_CHAT_ROOM_COUNT) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_LIMIT_EXCEEDED);
        }

        LocalDateTime createdAt = LocalDateTime.now();
        ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.create(
                member,
                request.getContent(),
                request.getSourceType(),
                createdAt
        ));
        ChatMessage message = chatMessageRepository.save(
                ChatMessage.createPendingUserText(
                        chatRoom,
                        request.getContent()
                )
        );

        return new ChatRoomCreateResponse(
                chatRoom.getId(),
                message.getId(),
                message.getCreatedAt()
        );
    }

    @Transactional
    public ChatMessageCreateResponse createMessage(
            Long memberId,
            Long chatRoomId,
            ChatMessageCreateRequest request
    ) {
        ChatRoom chatRoom = getOwnedActiveChatRoom(memberId, chatRoomId);

        boolean hasActiveGeneration = chatMessageRepository
                .existsByChatRoomIdAndSenderTypeAndGenerationStatusIn(
                        chatRoomId,
                        ChatSenderType.USER,
                        ACTIVE_STATUSES
                );
        if (hasActiveGeneration) {
            throw new BusinessException(ErrorCode.AI_RESPONSE_GENERATING);
        }

        ChatMessage message = chatMessageRepository.save(
                ChatMessage.createPendingUserText(
                        chatRoom,
                        request.getContent()
                )
        );
        chatRoom.updateLastMessageAt(LocalDateTime.now());

        return new ChatMessageCreateResponse(
                chatRoom.getId(),
                message.getId(),
                message.getContent(),
                message.getCreatedAt()
        );
    }

    private ChatRoom getOwnedActiveChatRoom(Long memberId, Long chatRoomId) {
        ChatRoom chatRoom = chatRoomRepository.findActiveByIdForUpdate(chatRoomId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.CHAT_ROOM_NOT_FOUND
                ));

        if (!chatRoom.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }

        return chatRoom;
    }
}
