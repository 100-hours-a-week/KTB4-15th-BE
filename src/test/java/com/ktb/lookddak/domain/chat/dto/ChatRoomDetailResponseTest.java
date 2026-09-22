package com.ktb.lookddak.domain.chat.dto;

import com.ktb.lookddak.domain.chat.entity.ChatMessage;
import com.ktb.lookddak.domain.chat.entity.ChatRoom;
import com.ktb.lookddak.domain.chat.entity.ChatSenderType;
import com.ktb.lookddak.domain.chat.entity.ChatSourceType;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.product.entity.Product;
import com.ktb.lookddak.domain.product.entity.ProductItemType;
import com.ktb.lookddak.domain.recommendation.entity.Recommendation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChatRoomDetailResponseTest {

    @Test
    @DisplayName("Product와 회원별 상태로 추천 상품 응답을 생성한다")
    void createRecommendedProductResponse() {
        Product product = createProduct(201L);

        RecommendedProductResponse response =
                RecommendedProductResponse.from(product, true, false);

        assertThat(response.getProductId()).isEqualTo(201L);
        assertThat(response.getProductName()).isEqualTo("에센셜 램스울 크루넥");
        assertThat(response.getProductImageUrl())
                .isEqualTo("https://image.lookddak.com/products/201.jpg");
        assertThat(response.getCurrentPrice()).isEqualTo(59_000);
        assertThat(response.getColor()).isEqualTo("네이비");
        assertThat(response.getItemType()).isEqualTo(ProductItemType.TOP);
        assertThat(response.getPurchaseUrl())
                .isEqualTo("https://shop.lookddak.com/products/201");
        assertThat(response.isWishlisted()).isTrue();
        assertThat(response.isFittingCandidate()).isFalse();
    }

    @Test
    @DisplayName("AI 추천 메시지를 중첩된 상세 응답으로 변환한다")
    void createRecommendationMessageResponse() {
        ChatRoom chatRoom = createChatRoom(123L);
        ChatMessage message = ChatMessage.createAiRecommendation(
                chatRoom,
                "출근할 때 입기 좋은 니트를 추천해드릴게요."
        );
        LocalDateTime createdAt = LocalDateTime.of(2026, 9, 8, 12, 30, 3);
        ReflectionTestUtils.setField(message, "id", 102L);
        ReflectionTestUtils.setField(message, "createdAt", createdAt);
        Recommendation recommendation = Recommendation.create(
                chatRoom.getMember(),
                message
        );
        ReflectionTestUtils.setField(recommendation, "id", 15L);
        RecommendationResponse recommendationResponse =
                RecommendationResponse.from(
                        recommendation,
                        List.of(RecommendedProductResponse.from(
                                createProduct(201L),
                                true,
                                false
                        ))
                );

        ChatMessageDetailResponse response = ChatMessageDetailResponse.from(
                message,
                recommendationResponse
        );

        assertThat(response.getMessageId()).isEqualTo(102L);
        assertThat(response.getSenderType()).isEqualTo(ChatSenderType.AI);
        assertThat(response.getGenerationStatus()).isNull();
        assertThat(response.getRecommendation().getRecommendationId())
                .isEqualTo(15L);
        assertThat(response.getRecommendation().getProducts()).hasSize(1);
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("채팅방과 메시지 페이지 정보로 상세 응답을 생성한다")
    void createChatRoomDetailResponse() {
        ChatRoom chatRoom = createChatRoom(123L);
        ChatMessage message = ChatMessage.createAiText(chatRoom, "AI 응답");
        ReflectionTestUtils.setField(message, "id", 105L);
        ChatMessageDetailResponse messageResponse =
                ChatMessageDetailResponse.from(message, null);
        List<ChatMessageDetailResponse> messages = new ArrayList<>();
        messages.add(messageResponse);

        ChatRoomDetailResponse response = ChatRoomDetailResponse.from(
                chatRoom,
                messages,
                105L,
                true
        );
        messages.clear();

        assertThat(response.getChatRoomId()).isEqualTo(123L);
        assertThat(response.getTitle()).isEqualTo("가을 출근용 니트 추천");
        assertThat(response.getMessages()).containsExactly(messageResponse);
        assertThat(response.getNextCursor()).isEqualTo(105L);
        assertThat(response.isHasNext()).isTrue();
    }

    private ChatRoom createChatRoom(Long id) {
        Member member = Member.create("member@lookddak.com", "encoded-password");
        ChatRoom chatRoom = ChatRoom.create(
                member,
                "가을 출근용 니트 추천",
                ChatSourceType.GENERAL,
                LocalDateTime.now()
        );
        ReflectionTestUtils.setField(chatRoom, "id", id);
        return chatRoom;
    }

    private Product createProduct(Long id) {
        Product product = Product.create(
                "에센셜 램스울 크루넥",
                "https://image.lookddak.com/products/201.jpg",
                59_000,
                "네이비",
                ProductItemType.TOP,
                "https://shop.lookddak.com/products/201"
        );
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }
}
