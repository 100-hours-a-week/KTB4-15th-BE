package com.ktb.lookddak.domain.wishlist.entity;

import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.product.entity.Product;
import com.ktb.lookddak.domain.product.entity.ProductItemType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class WishlistTest {

    @Test
    @DisplayName("찜을 생성하면 상품의 현재 가격을 저장한다")
    void createWishlist() {
        Member member = Member.create("member@lookddak.com", "encoded-password");
        ReflectionTestUtils.setField(member, "id", 1L);
        Product product = Product.create(
                "테스트 상품",
                "https://image.lookddak.com/test.jpg",
                49_000,
                "네이비",
                ProductItemType.TOP,
                "https://shop.lookddak.com/test"
        );

        Wishlist wishlist = Wishlist.create(member, product);

        assertThat(wishlist.getMember()).isSameAs(member);
        assertThat(wishlist.getProduct()).isSameAs(product);
        assertThat(wishlist.getWishedPrice()).isEqualTo(49_000);
        assertThat(wishlist.isOwnedBy(1L)).isTrue();
        assertThat(wishlist.isOwnedBy(2L)).isFalse();
    }
}
