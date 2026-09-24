package com.ktb.lookddak.domain.chat.service;

import com.ktb.lookddak.domain.chat.dto.ChatMessageCreateRequest;
import com.ktb.lookddak.domain.chat.dto.ChatMessageCreateResponse;
import com.ktb.lookddak.domain.chat.dto.ChatMessageDetailResponse;
import com.ktb.lookddak.domain.chat.dto.ChatGenerationStatusResponse;
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
                savedMessage.getId(),
                savedMessage.getCreatedAt()
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
                savedMessage.getContent(),
                savedMessage.getCreatedAt()
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

        List<ChatRoomListItemResponse> items = new ArrayList<>();
        for (ChatRoom responseRoom : responseRooms) {
            items.add(ChatRoomListItemResponse.from(responseRoom));
        }

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

        // 요청 개수보다 한 건 더 조회해 다음 페이지 존재 여부를 판단한다.
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

        // 현재 페이지에서 가장 오래된 메시지 ID를 다음 조회의 커서로 사용한다.
        Long nextCursor = hasNext && !pageMessages.isEmpty()
                ? pageMessages.get(pageMessages.size() - 1).getId()
                : null;

        // 현재 페이지에 포함된 추천 정보만 일괄 조회해 메시지별로 연결한다.
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

    public ChatGenerationStatusResponse getGenerationStatus(
            Long memberId,
            Long chatRoomId,
            Long messageId
    ) {
        getOwnedActiveChatRoomForRead(memberId, chatRoomId);

        ChatMessage userMessage = chatMessageRepository
                .findByIdAndChatRoomId(messageId, chatRoomId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.CHAT_MESSAGE_NOT_FOUND)
                );

        if (userMessage.getSenderType() != ChatSenderType.USER) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        ChatGenerationStatus generationStatus =
                userMessage.getGenerationStatus();
        if (generationStatus == null) {
            throw new IllegalStateException(
                    "USER 메시지의 생성 상태가 존재하지 않습니다."
            );
        }

        // 생성 중이거나 실패한 경우에는 AI 응답 관련 데이터를 조회하지 않는다.
        if (generationStatus != ChatGenerationStatus.COMPLETED) {
            return new ChatGenerationStatusResponse(generationStatus, null);
        }

        ChatMessage aiMessage = chatMessageRepository
                .findFirstByChatRoomIdAndIdGreaterThanOrderByIdAsc(
                        chatRoomId,
                        messageId
                )
                .orElseThrow(() ->
                        new IllegalStateException(
                                "완료된 USER 메시지의 AI 응답이 없습니다."
                        )
                );

        // 완료된 USER 메시지 바로 다음에는 최종 AI 메시지가 있어야 한다.
        if (aiMessage.getSenderType() != ChatSenderType.AI) {
            throw new IllegalStateException(
                    "완료된 USER 메시지 다음 메시지가 AI 응답이 아닙니다."
            );
        }

        Map<Long, RecommendationResponse> recommendationResponses =
                createRecommendationResponses(
                        memberId,
                        List.of(aiMessage)
                );
        RecommendationResponse recommendationResponse =
                recommendationResponses.get(aiMessage.getId());

        if (aiMessage.getMessageType() == ChatMessageType.RECOMMENDATION
                && recommendationResponse == null) {
            throw new IllegalStateException(
                    "추천 AI 메시지의 추천 데이터가 없습니다."
            );
        }

        ChatMessageDetailResponse messageResponse =
                ChatMessageDetailResponse.from(
                        aiMessage,
                        recommendationResponse
                );

        return new ChatGenerationStatusResponse(
                generationStatus,
                messageResponse
        );
    }

    private Map<Long, RecommendationResponse> createRecommendationResponses(
            Long memberId,
            List<ChatMessage> messages
    ) {
        // 일반 대화에는 추천 데이터가 없으므로 AI 추천 메시지만 선별한다.
        List<Long> recommendationMessageIds = new ArrayList<>();
        for (ChatMessage message : messages) {
            boolean isAiMessage =
                    message.getSenderType() == ChatSenderType.AI;
            boolean isRecommendationType =
                    message.getMessageType()
                            == ChatMessageType.RECOMMENDATION;

            if (isAiMessage && isRecommendationType) {
                recommendationMessageIds.add(message.getId());
            }
        }

        if (recommendationMessageIds.isEmpty()) {
            return Map.of();
        }

        List<Recommendation> recommendations = recommendationRepository
                .findAllByMessageIdIn(recommendationMessageIds);
        if (recommendations.isEmpty()) {
            return Map.of();
        }

        // 추천 상품을 IN 쿼리 한 번으로 조회하기 위해 추천 ID를 모은다.
        List<Long> recommendationIds = new ArrayList<>();
        for (Recommendation recommendation : recommendations) {
            recommendationIds.add(recommendation.getId());
        }

        List<RecommendationProduct> recommendationProducts =
                recommendationProductRepository
                        .findAllWithProductByRecommendationIdIn(
                                recommendationIds
                        );

        Map<Long, List<RecommendationProduct>> productsByRecommendationId =
                new LinkedHashMap<>();
        Set<Long> productIds = new LinkedHashSet<>();

        // Repository의 상품 ID 오름차순을 유지해 추천별 상품 목록을 만든다.
        // 동시에 찜·피팅 상태 조회에 사용할 상품 ID도 중복 없이 모은다.
        for (RecommendationProduct recommendationProduct
                : recommendationProducts) {
            Long recommendationId = recommendationProduct
                    .getRecommendation()
                    .getId();

            // 해당 추천의 상품 목록이 없으면 생성한 뒤 현재 상품을 추가한다.
            productsByRecommendationId
                    .computeIfAbsent(
                            recommendationId,
                            key -> new ArrayList<>()
                    )
                    .add(recommendationProduct);
            productIds.add(recommendationProduct.getProduct().getId());
        }

        // 상품마다 조회하지 않고 찜·피팅 상태를 각각 한 번의 쿼리로 가져온다.
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

        Map<Long, RecommendationResponse> responses = new LinkedHashMap<>();

        // 메시지 ID를 Key로 사용하면 메시지 응답을 만들 때 추천 정보를 바로 찾을 수 있다.
        for (Recommendation recommendation : recommendations) {
            List<RecommendationProduct> products =
                    productsByRecommendationId.getOrDefault(
                            recommendation.getId(),
                            List.of()
                    );
            List<RecommendedProductResponse> productResponses =
                    new ArrayList<>();

            for (RecommendationProduct recommendationProduct : products) {
                Long productId = recommendationProduct.getProduct().getId();
                productResponses.add(RecommendedProductResponse.from(
                        recommendationProduct,
                        wishlistedProductIds.contains(productId),
                        fittingCandidateProductIds.contains(productId)
                ));
            }

            responses.put(
                    recommendation.getMessage().getId(),
                    RecommendationResponse.from(
                            recommendation,
                            productResponses
                    )
            );
        }

        return responses;
    }

    private List<ChatMessageDetailResponse> createMessageResponses(
            List<ChatMessage> pageMessages,
            Map<Long, RecommendationResponse> recommendationResponses
    ) {
        List<ChatMessageDetailResponse> responses = new ArrayList<>();

        // DB에서는 최신순으로 조회하지만 화면에는 오래된 메시지부터 보여준다.
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
