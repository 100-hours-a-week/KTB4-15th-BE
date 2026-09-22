package com.ktb.lookddak.domain.recommendation.repository;

import com.ktb.lookddak.domain.chat.entity.ChatMessage;
import com.ktb.lookddak.domain.chat.entity.ChatRoom;
import com.ktb.lookddak.domain.chat.entity.ChatSourceType;
import com.ktb.lookddak.domain.chat.repository.ChatMessageRepository;
import com.ktb.lookddak.domain.chat.repository.ChatRoomRepository;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.member.repository.MemberRepository;
import com.ktb.lookddak.domain.product.entity.Product;
import com.ktb.lookddak.domain.product.entity.ProductItemType;
import com.ktb.lookddak.domain.product.repository.ProductRepository;
import com.ktb.lookddak.domain.recommendation.entity.Recommendation;
import com.ktb.lookddak.domain.recommendation.entity.RecommendationProduct;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(com.ktb.lookddak.global.config.JpaConfig.class)
class RecommendationRepositoryTest {

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Autowired
    private RecommendationProductRepository recommendationProductRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    private Member member;
    private ChatRoom chatRoom;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.create(
                "recommendation-repository@lookddak.com",
                "encoded-password"
        ));
        chatRoom = chatRoomRepository.save(ChatRoom.create(
                member,
                "가을 출근용 니트 추천해줘",
                ChatSourceType.GENERAL,
                LocalDateTime.now()
        ));
    }

    @Test
    @DisplayName("메시지 ID 목록에 연결된 추천을 일괄 조회한다")
    void findAllByMessageIdIn() {
        ChatMessage firstMessage = saveMessage("첫 번째 추천 요청");
        ChatMessage secondMessage = saveMessage("두 번째 추천 요청");
        Recommendation firstRecommendation = recommendationRepository.save(
                Recommendation.create(member, firstMessage)
        );
        recommendationRepository.saveAndFlush(
                Recommendation.create(member, secondMessage)
        );

        List<Recommendation> recommendations =
                recommendationRepository.findAllByMessageIdIn(
                        List.of(firstMessage.getId())
                );

        assertThat(recommendations)
                .extracting(Recommendation::getId)
                .containsExactly(firstRecommendation.getId());
    }

    @Test
    @DisplayName("한 메시지에는 추천을 하나만 연결할 수 있다")
    void enforceUniqueMessage() {
        ChatMessage message = saveMessage("추천 요청");
        recommendationRepository.saveAndFlush(
                Recommendation.create(member, message)
        );

        assertThatThrownBy(() -> recommendationRepository.saveAndFlush(
                Recommendation.create(member, message)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("추천 상품과 Product를 관계 PK 오름차순으로 일괄 조회한다")
    void findAllProductsInRecommendationOrder() {
        ChatMessage message = saveMessage("추천 요청");
        Recommendation recommendation = recommendationRepository.saveAndFlush(
                Recommendation.create(member, message)
        );
        Product firstRecommendedProduct = productRepository.save(
                createProduct("첫 번째 추천 상품", 59_000)
        );
        Product secondRecommendedProduct = productRepository.save(
                createProduct("두 번째 추천 상품", 69_000)
        );
        RecommendationProduct firstRelation =
                recommendationProductRepository.saveAndFlush(
                        RecommendationProduct.create(
                                recommendation,
                                firstRecommendedProduct
                        )
                );
        RecommendationProduct secondRelation =
                recommendationProductRepository.saveAndFlush(
                        RecommendationProduct.create(
                                recommendation,
                                secondRecommendedProduct
                        )
                );
        entityManager.clear();

        List<RecommendationProduct> recommendationProducts =
                recommendationProductRepository
                        .findAllWithProductByRecommendationIdIn(
                                List.of(recommendation.getId())
                        );

        assertThat(recommendationProducts)
                .extracting(RecommendationProduct::getId)
                .containsExactly(firstRelation.getId(), secondRelation.getId());
        assertThat(recommendationProducts)
                .allSatisfy(recommendationProduct ->
                        assertThat(Hibernate.isInitialized(
                                recommendationProduct.getProduct()
                        )).isTrue()
                );
    }

    @Test
    @DisplayName("같은 추천에 동일 상품을 중복 연결할 수 없다")
    void enforceUniqueRecommendationAndProduct() {
        ChatMessage message = saveMessage("추천 요청");
        Recommendation recommendation = recommendationRepository.saveAndFlush(
                Recommendation.create(member, message)
        );
        Product product = productRepository.save(createProduct("추천 상품", 59_000));
        recommendationProductRepository.saveAndFlush(
                RecommendationProduct.create(recommendation, product)
        );

        assertThatThrownBy(() ->
                recommendationProductRepository.saveAndFlush(
                        RecommendationProduct.create(recommendation, product)
                )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    private ChatMessage saveMessage(String content) {
        return chatMessageRepository.saveAndFlush(
                ChatMessage.createUserText(chatRoom, content)
        );
    }

    private Product createProduct(String name, Integer currentPrice) {
        return Product.create(
                name,
                "https://image.lookddak.com/test.jpg",
                currentPrice,
                "네이비",
                ProductItemType.TOP,
                "https://shop.lookddak.com/test"
        );
    }
}
