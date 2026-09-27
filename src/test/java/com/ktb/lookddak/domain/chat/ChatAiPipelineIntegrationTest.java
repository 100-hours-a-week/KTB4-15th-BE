package com.ktb.lookddak.domain.chat;

import com.ktb.lookddak.domain.chat.dto.ChatRoomCreateRequest;
import com.ktb.lookddak.domain.chat.dto.ChatRoomCreateResponse;
import com.ktb.lookddak.domain.chat.entity.ChatGenerationStatus;
import com.ktb.lookddak.domain.chat.entity.ChatMessage;
import com.ktb.lookddak.domain.chat.entity.ChatSenderType;
import com.ktb.lookddak.domain.chat.entity.ChatSourceType;
import com.ktb.lookddak.domain.chat.repository.ChatMessageRepository;
import com.ktb.lookddak.domain.chat.repository.ChatRoomRepository;
import com.ktb.lookddak.domain.chat.service.ChatService;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.member.repository.MemberRepository;
import com.ktb.lookddak.domain.product.entity.Product;
import com.ktb.lookddak.domain.product.entity.ProductItemType;
import com.ktb.lookddak.domain.product.repository.ProductRepository;
import com.ktb.lookddak.domain.recommendation.entity.RecommendationProduct;
import com.ktb.lookddak.domain.recommendation.repository.RecommendationProductRepository;
import com.ktb.lookddak.domain.recommendation.repository.RecommendationRepository;
import com.ktb.lookddak.global.client.ai.chat.AiChatClient;
import com.ktb.lookddak.global.client.ai.dto.AiChatDoneResponse;
import com.ktb.lookddak.global.client.ai.dto.AiChatRequest;
import com.ktb.lookddak.global.client.ai.dto.AiRecommendedProductResponse;
import com.ktb.lookddak.global.client.ai.exception.AiChatException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@SpringBootTest
class ChatAiPipelineIntegrationTest {

    private static final Duration ASYNC_TIMEOUT = Duration.ofSeconds(3);

    @Autowired
    private ChatService chatService;

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

    @MockitoBean
    private AiChatClient aiChatClient;

    @BeforeEach
    void cleanUp() {
        recommendationProductRepository.deleteAll();
        recommendationRepository.deleteAll();
        chatMessageRepository.deleteAll();
        chatRoomRepository.deleteAll();
        productRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("메시지 트랜잭션 커밋 후 AI 추천 응답을 비동기로 저장한다")
    void saveAiRecommendationAfterCommit() {
        Member member = saveMember("ai-pipeline-success@lookddak.com");
        given(aiChatClient.requestChat(any(AiChatRequest.class)))
                .willAnswer(invocation -> {
                    AiChatRequest request = invocation.getArgument(0);
                    return new AiChatDoneResponse(
                            request.getChatId(),
                            "조건에 맞는 상품을 찾아봤어요.",
                            List.of(new AiRecommendedProductResponse(
                                    "AI-0001",
                                    "오버핏 코튼 셔츠",
                                    "https://example.com/AI-0001.jpg",
                                    "https://shop.example.com/AI-0001",
                                    "NAVY",
                                    ProductItemType.TOP,
                                    69_000,
                                    "차분한 색감이 소개팅 룩에 잘 어울립니다."
                            ))
                    );
                });

        ChatRoomCreateResponse response = chatService.createChatRoom(
                member.getId(),
                new ChatRoomCreateRequest(
                        "소개팅용 네이비 셔츠 추천해줘",
                        ChatSourceType.GENERAL
                )
        );

        await(() -> isGenerationStatus(
                response.getMessageId(),
                ChatGenerationStatus.COMPLETED
        ));

        ChatMessage userMessage = chatMessageRepository
                .findById(response.getMessageId())
                .orElseThrow();
        List<ChatMessage> messages = chatMessageRepository.findAll();
        Product product = productRepository.findAll().getFirst();
        RecommendationProduct recommendationProduct =
                recommendationProductRepository.findAll().getFirst();

        assertThat(userMessage.getGenerationStatus())
                .isEqualTo(ChatGenerationStatus.COMPLETED);
        assertThat(messages).hasSize(2);
        assertThat(messages).anySatisfy(message -> {
            assertThat(message.getSenderType()).isEqualTo(ChatSenderType.AI);
            assertThat(message.getContent())
                    .isEqualTo("조건에 맞는 상품을 찾아봤어요.");
        });
        assertThat(product.getProductCode()).isEqualTo("AI-0001");
        assertThat(product.getCurrentPrice()).isEqualTo(69_000);
        assertThat(recommendationRepository.count()).isOne();
        assertThat(recommendationProduct.getPriceSnapshot()).isEqualTo(69_000);
        assertThat(recommendationProduct.getRecommendedReason())
                .isEqualTo("차분한 색감이 소개팅 룩에 잘 어울립니다.");
    }

    @Test
    @DisplayName("AI 호출이 실패하면 사용자 메시지를 FAILED로 변경한다")
    void failUserMessageWhenAiCallFails() {
        Member member = saveMember("ai-pipeline-failure@lookddak.com");
        given(aiChatClient.requestChat(any(AiChatRequest.class)))
                .willThrow(new AiChatException(
                        "recommendation_search_failed",
                        "상품 추천 처리 중 오류가 발생했습니다."
                ));

        ChatRoomCreateResponse response = chatService.createChatRoom(
                member.getId(),
                new ChatRoomCreateRequest(
                        "셔츠를 추천해줘",
                        ChatSourceType.GENERAL
                )
        );

        await(() -> isGenerationStatus(
                response.getMessageId(),
                ChatGenerationStatus.FAILED
        ));

        assertThat(chatMessageRepository.findAll()).hasSize(1);
        assertThat(productRepository.count()).isZero();
        assertThat(recommendationRepository.count()).isZero();
    }

    private Member saveMember(String email) {
        return memberRepository.saveAndFlush(Member.create(
                email,
                "encoded-password"
        ));
    }

    private boolean isGenerationStatus(
            Long messageId,
            ChatGenerationStatus expectedStatus
    ) {
        return chatMessageRepository.findById(messageId)
                .map(ChatMessage::getGenerationStatus)
                .filter(expectedStatus::equals)
                .isPresent();
    }

    private void await(BooleanSupplier condition) {
        Instant deadline = Instant.now().plus(ASYNC_TIMEOUT);

        while (Instant.now().isBefore(deadline)) {
            if (condition.getAsBoolean()) {
                return;
            }

            try {
                Thread.sleep(20);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AssertionError(
                        "비동기 AI 처리 결과를 기다리는 중 중단되었습니다.",
                        exception
                );
            }
        }

        throw new AssertionError("비동기 AI 처리 결과가 제한 시간 안에 저장되지 않았습니다.");
    }
}
