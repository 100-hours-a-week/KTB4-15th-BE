package com.ktb.lookddak.domain.chat.service;

import com.ktb.lookddak.domain.chat.dto.ChatMessageCreateRequest;
import com.ktb.lookddak.domain.chat.dto.ChatMessageCreateResponse;
import com.ktb.lookddak.domain.chat.dto.ChatMessageDetailResponse;
import com.ktb.lookddak.domain.chat.dto.ChatRoomDetailResponse;
import com.ktb.lookddak.domain.chat.dto.ChatRoomCreateRequest;
import com.ktb.lookddak.domain.chat.dto.ChatRoomCreateResponse;
import com.ktb.lookddak.domain.chat.dto.ChatRoomListItemResponse;
import com.ktb.lookddak.domain.chat.dto.ChatRoomListResponse;
import com.ktb.lookddak.domain.chat.dto.RecommendationResponse;
import com.ktb.lookddak.domain.chat.dto.RecommendedProductResponse;
import com.ktb.lookddak.domain.chat.dto.ChatRoomTitleUpdateRequest;
import com.ktb.lookddak.domain.chat.dto.ChatRoomTitleUpdateResponse;
import com.ktb.lookddak.domain.chat.entity.ChatGenerationStatus;
import com.ktb.lookddak.domain.chat.entity.ChatMessage;
import com.ktb.lookddak.domain.chat.entity.ChatMessageType;
import com.ktb.lookddak.domain.chat.entity.ChatRoom;
import com.ktb.lookddak.domain.chat.entity.ChatSenderType;
import com.ktb.lookddak.domain.chat.repository.ChatMessageRepository;
import com.ktb.lookddak.domain.chat.repository.ChatRoomRepository;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.member.repository.MemberRepository;
import com.ktb.lookddak.domain.fitting.repository.FittingCandidateRepository;
import com.ktb.lookddak.domain.recommendation.entity.Recommendation;
import com.ktb.lookddak.domain.recommendation.entity.RecommendationProduct;
import com.ktb.lookddak.domain.recommendation.repository.RecommendationProductRepository;
import com.ktb.lookddak.domain.recommendation.repository.RecommendationRepository;
import com.ktb.lookddak.domain.wishlist.repository.WishlistRepository;
import com.ktb.lookddak.global.exception.BusinessException;
import com.ktb.lookddak.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final RecommendationRepository recommendationRepository;
    private final RecommendationProductRepository recommendationProductRepository;
    private final WishlistRepository wishlistRepository;
    private final FittingCandidateRepository fittingCandidateRepository;

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

    public ChatRoomDetailResponse getChatRoomDetail(
            Long memberId,
            Long chatRoomId,
            Long cursor,
            Integer size
    ) {
        int pageSize = size == null ? DEFAULT_PAGE_SIZE : size;
        validatePagination(cursor, pageSize);

        ChatRoom chatRoom = getOwnedActiveChatRoomForRead(memberId, chatRoomId);
        validateMessageCursor(chatRoomId, cursor);

        PageRequest pageRequest = PageRequest.of(0, pageSize + 1);
        List<ChatMessage> fetchedMessages = cursor == null
                ? chatMessageRepository.findFirstPage(chatRoomId, pageRequest)
                : chatMessageRepository.findPreviousPage(
                        chatRoomId,
                        cursor,
                        pageRequest
                );

        boolean hasNext = fetchedMessages.size() > pageSize;
        List<ChatMessage> pageMessages = hasNext
                ? fetchedMessages.subList(0, pageSize)
                : fetchedMessages;
        Long nextCursor = hasNext && !pageMessages.isEmpty()
                ? pageMessages.get(pageMessages.size() - 1).getId()
                : null;

        Map<Long, RecommendationResponse> recommendationResponses =
                createRecommendationResponses(memberId, pageMessages);
        List<ChatMessageDetailResponse> messageResponses =
                createMessageResponses(pageMessages, recommendationResponses);

        return ChatRoomDetailResponse.from(
                chatRoom,
                messageResponses,
                nextCursor,
                hasNext
        );
    }

    private Map<Long, RecommendationResponse> createRecommendationResponses(
            Long memberId,
            List<ChatMessage> messages
    ) {
        List<Long> recommendationMessageIds = messages.stream()
                .filter(message -> message.getSenderType() == ChatSenderType.AI)
                .filter(message ->
                        message.getMessageType() == ChatMessageType.RECOMMENDATION
                )
                .map(ChatMessage::getId)
                .toList();

        if (recommendationMessageIds.isEmpty()) {
            return Map.of();
        }

        List<Recommendation> recommendations = recommendationRepository
                .findAllByMessageIdIn(recommendationMessageIds);
        if (recommendations.isEmpty()) {
            return Map.of();
        }

        List<Long> recommendationIds = recommendations.stream()
                .map(Recommendation::getId)
                .toList();
        List<RecommendationProduct> recommendationProducts =
                recommendationProductRepository
                        .findAllWithProductByRecommendationIdIn(
                                recommendationIds
                        );

        Map<Long, List<RecommendationProduct>> productsByRecommendationId =
                recommendationProducts.stream()
                        .collect(Collectors.groupingBy(
                                product -> product.getRecommendation().getId(),
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));

        Set<Long> productIds = recommendationProducts.stream()
                .map(product -> product.getProduct().getId())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<Long> wishlistedProductIds = productIds.isEmpty()
                ? Set.of()
                : wishlistRepository.findProductIdsByMemberIdAndProductIdIn(
                        memberId,
                        productIds
                );

        Set<Long> fittingCandidateProductIds = productIds.isEmpty()
                ? Set.of()
                : fittingCandidateRepository
                        .findProductIdsByMemberIdAndProductIdIn(
                                memberId,
                                productIds
                        );

        return recommendations.stream().collect(Collectors.toMap(
                recommendation -> recommendation.getMessage().getId(),
                recommendation -> RecommendationResponse.from(
                        recommendation,
                        productsByRecommendationId
                                .getOrDefault(recommendation.getId(), List.of())
                                .stream()
                                .map(product -> RecommendedProductResponse.from(
                                        product.getProduct(),
                                        wishlistedProductIds.contains(
                                                product.getProduct().getId()
                                        ),
                                        fittingCandidateProductIds.contains(
                                                product.getProduct().getId()
                                        )
                                ))
                                .toList()
                ),
                (existing, replacement) -> existing,
                LinkedHashMap::new
        ));
    }

    private List<ChatMessageDetailResponse> createMessageResponses(
            List<ChatMessage> pageMessages,
            Map<Long, RecommendationResponse> recommendationResponses
    ) {
        List<ChatMessageDetailResponse> responses = new ArrayList<>();

        for (int index = pageMessages.size() - 1; index >= 0; index--) {
            ChatMessage message = pageMessages.get(index);
            responses.add(ChatMessageDetailResponse.from(
                    message,
                    recommendationResponses.get(message.getId())
            ));
        }

        return responses;
    }

    private void validateMessageCursor(Long chatRoomId, Long cursor) {
        if (cursor != null && !chatMessageRepository
                .existsByIdAndChatRoomId(cursor, chatRoomId)) {
            throw new BusinessException(ErrorCode.INVALID_PAGINATION_PARAMETER);
        }
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

    private ChatRoom getOwnedActiveChatRoomForRead(
            Long memberId,
            Long chatRoomId
    ) {
        ChatRoom chatRoom = chatRoomRepository.findActiveById(chatRoomId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND)
                );

        if (!chatRoom.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }

        return chatRoom;
    }
}
