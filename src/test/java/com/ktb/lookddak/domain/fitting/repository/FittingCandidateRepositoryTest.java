package com.ktb.lookddak.domain.fitting.repository;

import com.ktb.lookddak.domain.fitting.entity.FittingCandidate;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.member.repository.MemberRepository;
import com.ktb.lookddak.domain.product.entity.Product;
import com.ktb.lookddak.domain.product.entity.ProductItemType;
import com.ktb.lookddak.domain.product.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

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

    @Autowired
    private EntityManager entityManager;

    private Member member;
    private Product product;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.create(
                "fitting-repository@lookddak.com",
                "encoded-password"
        ));
        product = productRepository.save(Product.create(
                "0000001",
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
    @DisplayName("여러 피팅 후보를 ID 오름차순으로 잠금 조회한다")
    void findAllByIdInForUpdate() {
        Member otherMember = memberRepository.save(Member.create(
                "other-lock-fitting@lookddak.com",
                "encoded-password"
        ));
        FittingCandidate first = saveCandidate(
                member,
                product
        );
        FittingCandidate second = saveCandidate(
                otherMember,
                createProduct("다른 회원 상품", 59_000)
        );
        flushAndClear();

        List<FittingCandidate> candidates = fittingCandidateRepository
                .findAllByIdInForUpdate(List.of(
                        second.getId(),
                        first.getId()
                ));

        assertThat(candidates)
                .extracting(FittingCandidate::getId)
                .containsExactly(first.getId(), second.getId());
    }

    @Test
    @DisplayName("회원의 피팅 후보 첫 페이지를 최신순으로 상품과 함께 조회한다")
    void findFirstPage() {
        FittingCandidate first = saveCandidate(
                member,
                createProduct("첫 번째 상의", 39_000, ProductItemType.TOP)
        );
        FittingCandidate second = saveCandidate(
                member,
                createProduct("두 번째 하의", 49_000, ProductItemType.BOTTOM)
        );
        FittingCandidate third = saveCandidate(
                member,
                createProduct("세 번째 상의", 59_000, ProductItemType.TOP)
        );
        Member otherMember = memberRepository.save(Member.create(
                "other-first-page@lookddak.com",
                "encoded-password"
        ));
        saveCandidate(
                otherMember,
                createProduct("다른 회원 상품", 69_000, ProductItemType.TOP)
        );
        flushAndClear();

        List<FittingCandidate> candidates = fittingCandidateRepository
                .findFirstPage(member.getId(), PageRequest.of(0, 2));

        assertThat(candidates)
                .extracting(FittingCandidate::getId)
                .containsExactly(third.getId(), second.getId());
        assertThat(candidates)
                .extracting(FittingCandidate::getId)
                .doesNotContain(first.getId());
        assertThat(Persistence.getPersistenceUtil().isLoaded(
                candidates.get(0),
                "product"
        )).isTrue();
    }

    @Test
    @DisplayName("Cursor보다 ID가 작은 피팅 후보를 최신순으로 조회한다")
    void findNextPage() {
        FittingCandidate first = saveCandidate(
                member,
                createProduct("첫 번째 상품", 39_000, ProductItemType.TOP)
        );
        FittingCandidate cursorCandidate = saveCandidate(
                member,
                createProduct("Cursor 상품", 49_000, ProductItemType.BOTTOM)
        );
        saveCandidate(
                member,
                createProduct("최신 상품", 59_000, ProductItemType.TOP)
        );
        flushAndClear();

        List<FittingCandidate> candidates = fittingCandidateRepository
                .findNextPage(
                        member.getId(),
                        cursorCandidate.getId(),
                        PageRequest.of(0, 10)
                );

        assertThat(candidates)
                .extracting(FittingCandidate::getId)
                .containsExactly(first.getId());
    }

    @Test
    @DisplayName("상품 타입에 해당하는 피팅 후보 첫 페이지만 조회한다")
    void findFirstPageByItemType() {
        FittingCandidate firstTop = saveCandidate(
                member,
                createProduct("첫 번째 상의", 39_000, ProductItemType.TOP)
        );
        saveCandidate(
                member,
                createProduct("하의", 49_000, ProductItemType.BOTTOM)
        );
        FittingCandidate secondTop = saveCandidate(
                member,
                createProduct("두 번째 상의", 59_000, ProductItemType.TOP)
        );
        flushAndClear();

        List<FittingCandidate> candidates = fittingCandidateRepository
                .findFirstPageByItemType(
                        member.getId(),
                        ProductItemType.TOP,
                        PageRequest.of(0, 10)
                );

        assertThat(candidates)
                .extracting(FittingCandidate::getId)
                .containsExactly(secondTop.getId(), firstTop.getId());
        assertThat(candidates)
                .extracting(candidate -> candidate.getProduct().getItemType())
                .containsOnly(ProductItemType.TOP);
    }

    @Test
    @DisplayName("상품 타입과 Cursor 조건을 함께 적용해 다음 페이지를 조회한다")
    void findNextPageByItemType() {
        FittingCandidate firstBottom = saveCandidate(
                member,
                createProduct("첫 번째 하의", 39_000, ProductItemType.BOTTOM)
        );
        saveCandidate(
                member,
                createProduct("상의", 49_000, ProductItemType.TOP)
        );
        FittingCandidate cursorCandidate = saveCandidate(
                member,
                createProduct("두 번째 하의", 59_000, ProductItemType.BOTTOM)
        );
        saveCandidate(
                member,
                createProduct("최신 하의", 69_000, ProductItemType.BOTTOM)
        );
        flushAndClear();

        List<FittingCandidate> candidates = fittingCandidateRepository
                .findNextPageByItemType(
                        member.getId(),
                        cursorCandidate.getId(),
                        ProductItemType.BOTTOM,
                        PageRequest.of(0, 10)
                );

        assertThat(candidates)
                .extracting(FittingCandidate::getId)
                .containsExactly(firstBottom.getId());
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

    private FittingCandidate saveCandidate(
            Member candidateMember,
            Product candidateProduct
    ) {
        Product savedProduct = productRepository.save(candidateProduct);
        return fittingCandidateRepository.save(
                FittingCandidate.create(candidateMember, savedProduct)
        );
    }

    private void flushAndClear() {
        fittingCandidateRepository.flush();
        entityManager.clear();
    }

    private Product createProduct(String name, Integer currentPrice) {
        return createProduct(name, currentPrice, ProductItemType.TOP);
    }

    private Product createProduct(
            String name,
            Integer currentPrice,
            ProductItemType itemType
    ) {
        return Product.create(
                "code-" + name,
                name,
                "https://image.lookddak.com/test.jpg",
                currentPrice,
                "네이비",
                itemType,
                "https://shop.lookddak.com/test"
        );
    }
}
