package com.ktb.lookddak.domain.fitting.repository;

import com.ktb.lookddak.domain.fitting.entity.FittingCandidate;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.member.repository.MemberRepository;
import com.ktb.lookddak.domain.product.entity.Product;
import com.ktb.lookddak.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

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
        product = productRepository.save(Product.create(49_000));
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
}
