package com.ktb.lookddak.domain.chat.service;

import com.ktb.lookddak.domain.chat.entity.ChatGenerationStatus;
import com.ktb.lookddak.domain.chat.entity.ChatMessage;
import com.ktb.lookddak.domain.chat.entity.ChatRoom;
import com.ktb.lookddak.domain.chat.entity.ChatSenderType;
import com.ktb.lookddak.domain.chat.entity.ChatSourceType;
import com.ktb.lookddak.domain.chat.event.ChatV2GenerationRequestedEvent;
import com.ktb.lookddak.domain.chat.repository.ChatMessageRepository;
import com.ktb.lookddak.domain.wishlist.repository.WishlistRepository;
import com.ktb.lookddak.global.exception.BusinessException;
import com.ktb.lookddak.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatV2GenerationStartService {

    private static final int AI_WISHLIST_PRODUCT_LIMIT = 10;

    private final ChatMessageRepository chatMessageRepository;
    private final WishlistRepository wishlistRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * PENDING 메시지 하나만 GENERATING으로 바꾸고, 실제 AI 스트림 시작은 커밋 후 이벤트로 넘긴다.
     * 이미 생성 중인 메시지는 재연결로 판단해 AI 요청을 중복 발행하지 않는다.
     */
    @Transactional
    public void startIfPending(Long userMessageId) {
        ChatMessage message = chatMessageRepository
                .findByIdWithChatRoomAndMember(userMessageId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.CHAT_MESSAGE_NOT_FOUND
                ));

        // 조건부 UPDATE는 clearAutomatically로 영속성 컨텍스트를 비운다.
        // 따라서 LAZY 연관관계가 필요한 값은 UPDATE 전에 일반 값으로 추출한다.
        ChatRoom chatRoom = message.getChatRoom();
        Long memberId = chatRoom.getMember().getId();
        Long chatRoomId = chatRoom.getId();
        String content = message.getContent();
        ChatSourceType chatRoomSourceType = chatRoom.getSourceType();

        int updatedCount = chatMessageRepository.updateGenerationStatusIfCurrent(
                userMessageId,
                ChatSenderType.USER,
                ChatGenerationStatus.PENDING,
                ChatGenerationStatus.GENERATING
        );

        if (updatedCount == 0) {
            return;
        }

        ChatSourceType sourceType = getAiSourceType(
                chatRoomId,
                message.getId(),
                chatRoomSourceType
        );
        List<String> productIds = getAiProductIds(
                memberId,
                sourceType
        );
        eventPublisher.publishEvent(new ChatV2GenerationRequestedEvent(
                memberId,
                chatRoomId,
                message.getId(),
                content,
                sourceType,
                productIds
        ));
    }

    private ChatSourceType getAiSourceType(
            Long chatRoomId,
            Long userMessageId,
            ChatSourceType chatRoomSourceType
    ) {
        if (chatRoomSourceType != ChatSourceType.WISHLIST) {
            return ChatSourceType.GENERAL;
        }

        boolean hasPreviousUserMessage = chatMessageRepository
                .existsByChatRoomIdAndSenderTypeAndIdLessThan(
                        chatRoomId,
                        ChatSenderType.USER,
                        userMessageId
                );

        return hasPreviousUserMessage
                ? ChatSourceType.GENERAL
                : ChatSourceType.WISHLIST;
    }

    private List<String> getAiProductIds(
            Long memberId,
            ChatSourceType sourceType
    ) {
        if (sourceType != ChatSourceType.WISHLIST) {
            return List.of();
        }

        return wishlistRepository.findRecentProductCodesByMemberId(
                memberId,
                PageRequest.of(0, AI_WISHLIST_PRODUCT_LIMIT)
        );
    }
}
