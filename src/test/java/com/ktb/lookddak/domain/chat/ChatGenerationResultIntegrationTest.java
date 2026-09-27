package com.ktb.lookddak.domain.chat;

import com.ktb.lookddak.domain.chat.entity.ChatGenerationStatus;
import com.ktb.lookddak.domain.chat.entity.ChatMessage;
import com.ktb.lookddak.domain.chat.entity.ChatMessageType;
import com.ktb.lookddak.domain.chat.entity.ChatRoom;
import com.ktb.lookddak.domain.chat.entity.ChatSourceType;
import com.ktb.lookddak.domain.chat.repository.ChatMessageRepository;
import com.ktb.lookddak.domain.chat.repository.ChatRoomRepository;
import com.ktb.lookddak.domain.chat.service.ChatGenerationResultService;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.member.repository.MemberRepository;
import com.ktb.lookddak.domain.product.entity.Product;
import com.ktb.lookddak.domain.product.entity.ProductItemType;
import com.ktb.lookddak.domain.product.repository.ProductRepository;
import com.ktb.lookddak.domain.recommendation.entity.Recommendation;
import com.ktb.lookddak.domain.recommendation.entity.RecommendationProduct;
import com.ktb.lookddak.domain.recommendation.repository.RecommendationProductRepository;
import com.ktb.lookddak.domain.recommendation.repository.RecommendationRepository;
import com.ktb.lookddak.global.client.ai.dto.AiChatDoneResponse;
import com.ktb.lookddak.global.client.ai.dto.AiRecommendedProductResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ChatGenerationResultIntegrationTest {

    @Autowired
    private ChatGenerationResultService resultService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Autowired
    private RecommendationProductRepository recommendationProductRepository;

    private Member member;
    private ChatRoom chatRoom;
    private ChatMessage userMessage;

    @BeforeEach
    void setUp() {
        member = memberRepository.saveAndFlush(Member.create(
                "ai-result@lookddak.com",
                "encoded-password"
        ));
        chatRoom = chatRoomRepository.saveAndFlush(ChatRoom.create(
                member,
                "네이비 셔츠 추천해줘",
                ChatSourceType.GENERAL,
                LocalDateTime.now()
        ));
        userMessage = chatMessageRepository.saveAndFlush(
                ChatMessage.createUserText(
                        chatRoom,
                        "네이비 셔츠 추천해줘"
                )
        );
    }

    @Test
    @DisplayName("AI 텍스트 응답을 저장하고 사용자 메시지를 완료한다")
    void completeTextResponse() {
        resultService.complete(
                userMessage.getId(),
                new AiChatDoneResponse(
                        chatRoom.getId(),
                        "소개팅용 네이비 셔츠로 추천해드릴까요?",
                        List.of()
                )
        );

        ChatMessage savedUserMessage = chatMessageRepository
                .findById(userMessage.getId())
                .orElseThrow();
        ChatMessage aiMessage = chatMessageRepository
                .findFirstByChatRoomIdAndIdGreaterThanOrderByIdAsc(
                        chatRoom.getId(),
                        userMessage.getId()
                )
                .orElseThrow();

        assertThat(savedUserMessage.getGenerationStatus())
                .isEqualTo(ChatGenerationStatus.COMPLETED);
        assertThat(aiMessage.getMessageType()).isEqualTo(ChatMessageType.TEXT);
        assertThat(aiMessage.getContent())
                .isEqualTo("소개팅용 네이비 셔츠로 추천해드릴까요?");
        assertThat(recommendationRepository
                .findAllByMessageIdIn(List.of(aiMessage.getId()))).isEmpty();
        assertThat(chatRoom.getLastMessageAt())
                .isEqualTo(aiMessage.getCreatedAt());
    }

    @Test
    @DisplayName("기존 상품은 갱신하지 않고 없는 상품만 생성해 추천 스냅샷을 저장한다")
    void completeRecommendationResponse() {
        Product existingProduct = productRepository.saveAndFlush(
                Product.create(
                        "0000001",
                        "동기화된 기존 상품명",
                        "https://sync.example.com/1.jpg",
                        50_000,
                        "NAVY",
                        ProductItemType.TOP,
                        "https://sync.example.com/1"
                )
        );
        List<AiRecommendedProductResponse> aiProducts = List.of(
                aiProduct(
                        "0000001",
                        "AI가 보낸 변경 상품명",
                        69_000,
                        "첫 번째 추천 이유"
                ),
                aiProduct(
                        "0000002",
                        "새로운 셔츠",
                        59_000,
                        "두 번째 추천 이유"
                )
        );

        resultService.complete(
                userMessage.getId(),
                new AiChatDoneResponse(
                        chatRoom.getId(),
                        "조건에 맞는 상품을 찾아봤어요.",
                        aiProducts
                )
        );
        chatMessageRepository.flush();

        Product unchangedProduct = productRepository
                .findAllByProductCodeIn(List.of("0000001"))
                .getFirst();
        Product newProduct = productRepository
                .findAllByProductCodeIn(List.of("0000002"))
                .getFirst();
        ChatMessage aiMessage = chatMessageRepository
                .findFirstByChatRoomIdAndIdGreaterThanOrderByIdAsc(
                        chatRoom.getId(),
                        userMessage.getId()
                )
                .orElseThrow();
        Recommendation recommendation = recommendationRepository
                .findAllByMessageIdIn(List.of(aiMessage.getId()))
                .getFirst();
        List<RecommendationProduct> recommendationProducts =
                recommendationProductRepository
                        .findAllWithProductByRecommendationIdIn(
                                List.of(recommendation.getId())
                        );

        assertThat(unchangedProduct.getId()).isEqualTo(existingProduct.getId());
        assertThat(unchangedProduct.getName())
                .isEqualTo("동기화된 기존 상품명");
        assertThat(unchangedProduct.getCurrentPrice()).isEqualTo(50_000);
        assertThat(newProduct.getName()).isEqualTo("새로운 셔츠");
        assertThat(newProduct.getCurrentPrice()).isEqualTo(59_000);
        assertThat(aiMessage.getMessageType())
                .isEqualTo(ChatMessageType.RECOMMENDATION);
        assertThat(recommendationProducts)
                .extracting(RecommendationProduct::getPriceSnapshot)
                .containsExactly(69_000, 59_000);
        assertThat(recommendationProducts)
                .extracting(RecommendationProduct::getRecommendedReason)
                .containsExactly("첫 번째 추천 이유", "두 번째 추천 이유");
        assertThat(userMessage.getGenerationStatus())
                .isEqualTo(ChatGenerationStatus.COMPLETED);
    }

    @Test
    @DisplayName("AI 응답 생성 실패 시 사용자 메시지만 실패 상태로 변경한다")
    void failGeneration() {
        resultService.fail(userMessage.getId());

        assertThat(userMessage.getGenerationStatus())
                .isEqualTo(ChatGenerationStatus.FAILED);
        assertThat(chatMessageRepository
                .findFirstByChatRoomIdAndIdGreaterThanOrderByIdAsc(
                        chatRoom.getId(),
                        userMessage.getId()
                )).isEmpty();
    }

    @Test
    @DisplayName("이미 완료된 사용자 메시지의 중복 AI 결과는 다시 저장하지 않는다")
    void ignoreDuplicatedCompletedResult() {
        AiChatDoneResponse response = new AiChatDoneResponse(
                chatRoom.getId(),
                "최종 AI 응답",
                List.of()
        );
        resultService.complete(userMessage.getId(), response);
        resultService.complete(userMessage.getId(), response);

        List<ChatMessage> messages = chatMessageRepository.findFirstPage(
                chatRoom.getId(),
                org.springframework.data.domain.PageRequest.of(0, 10)
        );

        assertThat(messages).hasSize(2);
        assertThat(userMessage.getGenerationStatus())
                .isEqualTo(ChatGenerationStatus.COMPLETED);
    }

    private AiRecommendedProductResponse aiProduct(
            String productCode,
            String productName,
            Integer price,
            String reason
    ) {
        return new AiRecommendedProductResponse(
                productCode,
                productName,
                "https://ai.example.com/" + productCode + ".jpg",
                "https://shop.example.com/" + productCode,
                "NAVY",
                ProductItemType.TOP,
                price,
                reason
        );
    }
}
