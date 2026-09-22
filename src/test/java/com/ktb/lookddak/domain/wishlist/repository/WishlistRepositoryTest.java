package com.ktb.lookddak.domain.wishlist.repository;

import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.member.repository.MemberRepository;
import com.ktb.lookddak.domain.product.entity.Product;
import com.ktb.lookddak.domain.product.entity.ProductItemType;
import com.ktb.lookddak.domain.product.repository.ProductRepository;
import com.ktb.lookddak.domain.wishlist.entity.Wishlist;
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
class WishlistRepositoryTest {

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProductRepository productRepository;

    private Member member;
    private Product product;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.create(
                "wishlist-repository@lookddak.com",
                "encoded-password"
        ));
        product = productRepository.save(createProduct("상품 1", 49_000));
    }

    @Test
    @DisplayName("회원과 상품으로 찜 존재 여부를 확인한다")
    void existsByMemberAndProduct() {
        wishlistRepository.saveAndFlush(Wishlist.create(member, product));

        boolean exists = wishlistRepository.existsByMemberIdAndProductId(
                member.getId(),
                product.getId()
        );

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("현재 회원의 찜만 집계한다")
    void countByMember() {
        Member otherMember = memberRepository.save(Member.create(
                "other-wishlist@lookddak.com",
                "encoded-password"
        ));
        Product secondProduct = productRepository.save(
                createProduct("상품 2", 59_000)
        );
        wishlistRepository.save(Wishlist.create(member, product));
        wishlistRepository.save(Wishlist.create(member, secondProduct));
        wishlistRepository.saveAndFlush(Wishlist.create(otherMember, product));

        long count = wishlistRepository.countByMemberId(member.getId());

        assertThat(count).isEqualTo(2L);
    }

    @Test
    @DisplayName("상품 ID 목록 중 현재 회원이 찜한 상품 ID만 조회한다")
    void findWishlistedProductIds() {
        Member otherMember = memberRepository.save(Member.create(
                "other-bulk-wishlist@lookddak.com",
                "encoded-password"
        ));
        Product otherMembersProduct = productRepository.save(
                createProduct("다른 회원 찜 상품", 59_000)
        );
        Product notWishlistedProduct = productRepository.save(
                createProduct("찜하지 않은 상품", 69_000)
        );
        wishlistRepository.save(Wishlist.create(member, product));
        wishlistRepository.saveAndFlush(
                Wishlist.create(otherMember, otherMembersProduct)
        );

        Set<Long> productIds = wishlistRepository
                .findProductIdsByMemberIdAndProductIdIn(
                        member.getId(),
                        List.of(
                                product.getId(),
                                otherMembersProduct.getId(),
                                notWishlistedProduct.getId()
                        )
                );

        assertThat(productIds).containsExactly(product.getId());
    }

    @Test
    @DisplayName("한 회원이 같은 상품을 중복으로 찜할 수 없다")
    void enforceUniqueMemberAndProduct() {
        wishlistRepository.saveAndFlush(Wishlist.create(member, product));

        assertThatThrownBy(() -> wishlistRepository.saveAndFlush(
                Wishlist.create(member, product)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("찜을 삭제하면 데이터가 물리적으로 제거된다")
    void hardDeleteWishlist() {
        Wishlist wishlist = wishlistRepository.saveAndFlush(
                Wishlist.create(member, product)
        );

        wishlistRepository.delete(wishlist);
        wishlistRepository.flush();

        assertThat(wishlistRepository.findById(wishlist.getId())).isEmpty();
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
