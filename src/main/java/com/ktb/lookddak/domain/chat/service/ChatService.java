package com.ktb.lookddak.domain.chat.service;

import com.ktb.lookddak.domain.chat.dto.ChatMessageCreateRequest;
import com.ktb.lookddak.domain.chat.dto.ChatMessageCreateResponse;
import com.ktb.lookddak.domain.chat.dto.ChatRoomCreateRequest;
import com.ktb.lookddak.domain.chat.dto.ChatRoomCreateResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

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
        ChatRoom chatRoom = chatRoomRepository.findByIdForUpdate(chatRoomId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND)
                );

        if (!chatRoom.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }

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
}
