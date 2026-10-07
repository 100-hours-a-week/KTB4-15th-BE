package com.ktb.lookddak.domain.chat.service;

import com.ktb.lookddak.domain.chat.entity.ChatGenerationStatus;
import com.ktb.lookddak.domain.chat.entity.ChatMessage;
import com.ktb.lookddak.domain.chat.entity.ChatSenderType;
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
        ChatMessage message = chatMessageRepository.findById(userMessageId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.CHAT_MESSAGE_NOT_FOUND
                ));

        int updatedCount = chatMessageRepository.updateGenerationStatusIfCurrent(
                userMessageId,
                ChatSenderType.USER,
                ChatGenerationStatus.PENDING,
                ChatGenerationStatus.GENERATING
        );

        if (updatedCount == 0) {
            return;
        }

        Long memberId = message.getChatRoom().getMember().getId();
        List<String> productIds = getAiProductIds(
                memberId,
                message.getChatRoom().getSourceType()
        );
        eventPublisher.publishEvent(new ChatV2GenerationRequestedEvent(
                memberId,
                message.getChatRoom().getId(),
                message.getId(),
                message.getContent(),
                message.getChatRoom().getSourceType(),
                productIds
        ));
    }

    private List<String> getAiProductIds(
            Long memberId,
            com.ktb.lookddak.domain.chat.entity.ChatSourceType sourceType
    ) {
        if (sourceType != com.ktb.lookddak.domain.chat.entity.ChatSourceType.WISHLIST) {
            return List.of();
        }

        return wishlistRepository.findRecentProductCodesByMemberId(
                memberId,
                PageRequest.of(0, AI_WISHLIST_PRODUCT_LIMIT)
        );
    }
}
