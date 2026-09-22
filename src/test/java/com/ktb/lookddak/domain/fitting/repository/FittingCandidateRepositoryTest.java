package com.ktb.lookddak.domain.fitting.repository;

import com.ktb.lookddak.domain.fitting.entity.FittingCandidate;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.member.repository.MemberRepository;
import com.ktb.lookddak.domain.product.entity.Product;
import com.ktb.lookddak.domain.product.entity.ProductItemType;
import com.ktb.lookddak.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(com.ktb.lookddak.global.config.JpaConfig.class)
class FittingCandidateRepositoryTest {

    @Autowired
    private FittingCandidateRepository fittingCandidateRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProductRepository productRepository;

    private Member member;
    private Product product;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.create(
                "fitting-repository@lookddak.com",
                "encoded-password"
        ));
        product = productRepository.save(Product.create(
                "에센셜 램스울 크루넥",
                "https://image.lookddak.com/products/1.jpg",
                49_000,
                "네이비",
                ProductItemType.TOP,
                "https://shop.lookddak.com/products/1"
        ));
    }

    @Test
    @DisplayName("회원과 상품으로 중복 여부를 확인하고 회원별 개수를 조회한다")
    void existsAndCountByMember() {
        fittingCandidateRepository.saveAndFlush(
                FittingCandidate.create(member, product)
        );

        assertThat(fittingCandidateRepository
                .existsByMemberIdAndProductId(member.getId(), product.getId()))
                .isTrue();
        assertThat(fittingCandidateRepository.countByMemberId(member.getId()))
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("상품 ID 목록 중 현재 회원이 피팅 후보로 선택한 상품 ID만 조회한다")
    void findFittingCandidateProductIds() {
        Member otherMember = memberRepository.save(Member.create(
                "other-bulk-fitting@lookddak.com",
                "encoded-password"
        ));
        Product otherMembersProduct = productRepository.save(
                createProduct("다른 회원 후보 상품", 59_000)
        );
        Product notSelectedProduct = productRepository.save(
                createProduct("선택하지 않은 상품", 69_000)
        );
        fittingCandidateRepository.save(
                FittingCandidate.create(member, product)
        );
        fittingCandidateRepository.saveAndFlush(
                FittingCandidate.create(otherMember, otherMembersProduct)
        );

        Set<Long> productIds = fittingCandidateRepository
                .findProductIdsByMemberIdAndProductIdIn(
                        member.getId(),
                        List.of(
                                product.getId(),
                                otherMembersProduct.getId(),
                                notSelectedProduct.getId()
                        )
                );

        assertThat(productIds).containsExactly(product.getId());
    }

    @Test
    @DisplayName("한 회원은 동일 상품을 중복으로 추가할 수 없다")
    void enforceUniqueMemberAndProduct() {
        fittingCandidateRepository.saveAndFlush(
                FittingCandidate.create(member, product)
        );

        assertThatThrownBy(() -> fittingCandidateRepository.saveAndFlush(
                FittingCandidate.create(member, product)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("후보를 하드 삭제해도 상품은 유지된다")
    void hardDeleteCandidateOnly() {
        FittingCandidate candidate = fittingCandidateRepository.saveAndFlush(
                FittingCandidate.create(member, product)
        );

        fittingCandidateRepository.delete(candidate);
        fittingCandidateRepository.flush();

        assertThat(fittingCandidateRepository.findById(candidate.getId()))
                .isEmpty();
        assertThat(productRepository.findById(product.getId())).isPresent();
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
