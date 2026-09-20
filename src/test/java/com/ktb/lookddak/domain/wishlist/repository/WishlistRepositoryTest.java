package com.ktb.lookddak.domain.wishlist.repository;

import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.member.repository.MemberRepository;
import com.ktb.lookddak.domain.product.entity.Product;
import com.ktb.lookddak.domain.product.repository.ProductRepository;
import com.ktb.lookddak.domain.wishlist.entity.Wishlist;
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
        product = productRepository.save(Product.create(49_000));
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
}
