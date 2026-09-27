package com.ktb.lookddak.domain.chat.service;

import com.ktb.lookddak.domain.chat.entity.ChatGenerationStatus;
import com.ktb.lookddak.domain.chat.entity.ChatMessage;
import com.ktb.lookddak.domain.chat.entity.ChatRoom;
import com.ktb.lookddak.domain.chat.entity.ChatSenderType;
import com.ktb.lookddak.domain.chat.repository.ChatMessageRepository;
import com.ktb.lookddak.domain.product.entity.Product;
import com.ktb.lookddak.domain.product.repository.ProductRepository;
import com.ktb.lookddak.domain.recommendation.entity.Recommendation;
import com.ktb.lookddak.domain.recommendation.entity.RecommendationProduct;
import com.ktb.lookddak.domain.recommendation.repository.RecommendationProductRepository;
import com.ktb.lookddak.domain.recommendation.repository.RecommendationRepository;
import com.ktb.lookddak.global.client.ai.dto.AiChatDoneResponse;
import com.ktb.lookddak.global.client.ai.dto.AiRecommendedProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatGenerationResultService {

    private final ChatMessageRepository chatMessageRepository;
    private final ProductRepository productRepository;
    private final RecommendationRepository recommendationRepository;
    private final RecommendationProductRepository recommendationProductRepository;

    @Transactional
    public void complete(
            Long userMessageId,
            AiChatDoneResponse response
    ) {
        ChatMessage userMessage = getUserMessageForUpdate(userMessageId);
        if (!isGenerating(userMessage)) {
            return;
        }

        ChatRoom chatRoom = userMessage.getChatRoom();
        validateResponseChatRoom(chatRoom.getId(), response.getChatId());

        ChatMessage aiMessage = response.getProducts().isEmpty()
                ? ChatMessage.createAiText(chatRoom, response.getContent())
                : ChatMessage.createAiRecommendation(
                        chatRoom,
                        response.getContent()
                );
        ChatMessage savedAiMessage = chatMessageRepository.saveAndFlush(
                aiMessage
        );

        if (!response.getProducts().isEmpty()) {
            saveRecommendation(
                    chatRoom,
                    savedAiMessage,
                    response.getProducts()
            );
        }

        userMessage.completeGeneration();
        chatRoom.updateLastMessageAt(savedAiMessage.getCreatedAt());
    }

    @Transactional
    public void fail(Long userMessageId) {
        ChatMessage userMessage = getUserMessageForUpdate(userMessageId);
        if (!isGenerating(userMessage)) {
            return;
        }

        userMessage.failGeneration();
    }

    private void saveRecommendation(
            ChatRoom chatRoom,
            ChatMessage aiMessage,
            List<AiRecommendedProductResponse> aiProducts
    ) {
        Map<String, AiRecommendedProductResponse> aiProductByCode =
                createUniqueProductMap(aiProducts);
        Map<String, Product> productByCode = findExistingProducts(
                new ArrayList<>(aiProductByCode.keySet())
        );

        List<Product> newProducts = new ArrayList<>();
        for (AiRecommendedProductResponse aiProduct
                : aiProductByCode.values()) {
            if (!productByCode.containsKey(aiProduct.getProductCode())) {
                Product newProduct = createProduct(aiProduct);
                newProducts.add(newProduct);
                productByCode.put(newProduct.getProductCode(), newProduct);
            }
        }
        productRepository.saveAll(newProducts);

        Recommendation recommendation = recommendationRepository.save(
                Recommendation.create(chatRoom.getMember(), aiMessage)
        );
        List<RecommendationProduct> recommendationProducts =
                new ArrayList<>();

        // AI가 반환한 순서를 유지해 추천 상품 관계를 생성한다.
        for (AiRecommendedProductResponse aiProduct
                : aiProductByCode.values()) {
            Product product = productByCode.get(aiProduct.getProductCode());
            recommendationProducts.add(RecommendationProduct.create(
                    recommendation,
                    product,
                    aiProduct.getPrice(),
                    aiProduct.getLlmComment()
            ));
        }
        recommendationProductRepository.saveAll(recommendationProducts);
    }

    private Map<String, AiRecommendedProductResponse> createUniqueProductMap(
            List<AiRecommendedProductResponse> aiProducts
    ) {
        Map<String, AiRecommendedProductResponse> products =
                new LinkedHashMap<>();

        for (AiRecommendedProductResponse product : aiProducts) {
            if (products.putIfAbsent(product.getProductCode(), product)
                    != null) {
                throw new IllegalStateException(
                        "AI 추천 결과에 중복된 상품 코드가 있습니다."
                );
            }
        }
        return products;
    }

    private Map<String, Product> findExistingProducts(
            List<String> productCodes
    ) {
        Map<String, Product> products = new LinkedHashMap<>();
        for (Product product
                : productRepository.findAllByProductCodeIn(productCodes)) {
            products.put(product.getProductCode(), product);
        }
        return products;
    }

    private Product createProduct(AiRecommendedProductResponse aiProduct) {
        return Product.create(
                aiProduct.getProductCode(),
                aiProduct.getProductName(),
                aiProduct.getImageUrl(),
                aiProduct.getPrice(),
                aiProduct.getColor(),
                aiProduct.getItemType(),
                aiProduct.getDetailUrl()
        );
    }

    private ChatMessage getUserMessageForUpdate(Long userMessageId) {
        ChatMessage message = chatMessageRepository
                .findByIdForGenerationUpdate(userMessageId)
                .orElseThrow(() -> new IllegalStateException(
                        "AI 응답을 연결할 사용자 메시지가 없습니다."
                ));

        if (message.getSenderType() != ChatSenderType.USER) {
            throw new IllegalStateException(
                    "AI 응답은 사용자 메시지에만 연결할 수 있습니다."
            );
        }
        return message;
    }

    private boolean isGenerating(ChatMessage message) {
        return message.getGenerationStatus()
                == ChatGenerationStatus.GENERATING;
    }

    private void validateResponseChatRoom(
            Long requestedChatRoomId,
            Long responseChatRoomId
    ) {
        if (!requestedChatRoomId.equals(responseChatRoomId)) {
            throw new IllegalStateException(
                    "사용자 메시지와 AI 응답의 채팅방이 일치하지 않습니다."
            );
        }
    }
}
