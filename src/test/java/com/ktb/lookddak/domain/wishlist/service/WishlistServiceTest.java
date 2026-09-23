package com.ktb.lookddak.domain.wishlist.service;

import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.member.repository.MemberRepository;
import com.ktb.lookddak.domain.product.entity.Product;
import com.ktb.lookddak.domain.product.entity.ProductItemType;
import com.ktb.lookddak.domain.product.repository.ProductRepository;
import com.ktb.lookddak.domain.wishlist.dto.WishlistCreateRequest;
import com.ktb.lookddak.domain.wishlist.dto.WishlistCreateResponse;
import com.ktb.lookddak.domain.wishlist.dto.WishlistCountResponse;
import com.ktb.lookddak.domain.wishlist.entity.Wishlist;
import com.ktb.lookddak.domain.wishlist.repository.WishlistRepository;
import com.ktb.lookddak.global.exception.BusinessException;
import com.ktb.lookddak.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WishlistRepository wishlistRepository;

    private WishlistService wishlistService;

    @BeforeEach
    void setUp() {
        wishlistService = new WishlistService(
                memberRepository,
                productRepository,
                wishlistRepository
        );
    }

    @Test
    @DisplayName("현재 회원의 찜 개수를 조회한다")
    void getWishlistCount() {
        given(wishlistRepository.countByMemberId(1L)).willReturn(3L);

        WishlistCountResponse response = wishlistService.getWishlistCount(1L);

        assertThat(response.getCount()).isEqualTo(3L);
        verify(wishlistRepository).countByMemberId(1L);
        verify(memberRepository, never()).findById(any());
    }

    @Test
    @DisplayName("찜이 없으면 개수로 0을 반환한다")
    void getEmptyWishlistCount() {
        given(wishlistRepository.countByMemberId(1L)).willReturn(0L);

        WishlistCountResponse response = wishlistService.getWishlistCount(1L);

        assertThat(response.getCount()).isZero();
    }

    @Test
    @DisplayName("상품을 찜하고 현재 가격을 저장한다")
    void createWishlist() {
        Member member = createMember(1L);
        Product product = createProduct(10L, 49_000);
        given(memberRepository.findByIdAndDeletedAtIsNull(1L))
                .willReturn(Optional.of(member));
        given(productRepository.findById(10L)).willReturn(Optional.of(product));
        given(wishlistRepository.existsByMemberIdAndProductId(1L, 10L))
                .willReturn(false);
        given(wishlistRepository.saveAndFlush(any(Wishlist.class)))
                .willAnswer(invocation -> {
                    Wishlist wishlist = invocation.getArgument(0);
                    ReflectionTestUtils.setField(wishlist, "id", 100L);
                    return wishlist;
                });

        WishlistCreateResponse response = wishlistService.createWishlist(
                1L,
                new WishlistCreateRequest(10L)
        );

        assertThat(response.getWishlistId()).isEqualTo(100L);
        assertThat(response.getProductId()).isEqualTo(10L);
        ArgumentCaptor<Wishlist> captor = ArgumentCaptor.forClass(Wishlist.class);
        verify(wishlistRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getWishedPrice()).isEqualTo(49_000);
    }

    @Test
    @DisplayName("존재하지 않는 상품은 찜할 수 없다")
    void rejectMissingProduct() {
        given(memberRepository.findByIdAndDeletedAtIsNull(1L))
                .willReturn(Optional.of(createMember(1L)));
        given(productRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> wishlistService.createWishlist(
                1L,
                new WishlistCreateRequest(10L)
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND)
        );

        verify(wishlistRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("이미 찜한 상품을 중복으로 찜할 수 없다")
    void rejectDuplicatedWishlist() {
        Member member = createMember(1L);
        Product product = createProduct(10L, 49_000);
        given(memberRepository.findByIdAndDeletedAtIsNull(1L))
                .willReturn(Optional.of(member));
        given(productRepository.findById(10L)).willReturn(Optional.of(product));
        given(wishlistRepository.existsByMemberIdAndProductId(1L, 10L))
                .willReturn(true);

        assertThatThrownBy(() -> wishlistService.createWishlist(
                1L,
                new WishlistCreateRequest(10L)
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.WISHLIST_ALREADY_EXISTS)
        );
    }

    @Test
    @DisplayName("동시 요청으로 UNIQUE 제약이 충돌하면 중복 찜 오류로 변환한다")
    void handleConcurrentDuplicate() {
        Member member = createMember(1L);
        Product product = createProduct(10L, 49_000);
        given(memberRepository.findByIdAndDeletedAtIsNull(1L))
                .willReturn(Optional.of(member));
        given(productRepository.findById(10L)).willReturn(Optional.of(product));
        given(wishlistRepository.existsByMemberIdAndProductId(1L, 10L))
                .willReturn(false);
        given(wishlistRepository.saveAndFlush(any(Wishlist.class)))
                .willThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> wishlistService.createWishlist(
                1L,
                new WishlistCreateRequest(10L)
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.WISHLIST_ALREADY_EXISTS)
        );
    }

    @Test
    @DisplayName("본인의 찜을 하드 삭제한다")
    void deleteWishlist() {
        Wishlist wishlist = createWishlist(100L, createMember(1L));
        given(wishlistRepository.findByIdForUpdate(100L))
                .willReturn(Optional.of(wishlist));

        wishlistService.deleteWishlist(1L, 100L);

        verify(wishlistRepository).delete(wishlist);
    }

    @Test
    @DisplayName("다른 회원의 찜은 삭제할 수 없다")
    void rejectOtherMembersWishlist() {
        Wishlist wishlist = createWishlist(100L, createMember(2L));
        given(wishlistRepository.findByIdForUpdate(100L))
                .willReturn(Optional.of(wishlist));

        assertThatThrownBy(() -> wishlistService.deleteWishlist(1L, 100L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.WISHLIST_ACCESS_DENIED)
                );

        verify(wishlistRepository, never()).delete(any());
    }

    private Member createMember(Long id) {
        Member member = Member.create("member" + id + "@lookddak.com", "encoded");
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private Product createProduct(Long id, Integer currentPrice) {
        Product product = Product.create(
                "테스트 상품",
                "https://image.lookddak.com/test.jpg",
                currentPrice,
                "네이비",
                ProductItemType.TOP,
                "https://shop.lookddak.com/test"
        );
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    private Wishlist createWishlist(Long id, Member member) {
        Wishlist wishlist = Wishlist.create(member, createProduct(10L, 49_000));
        ReflectionTestUtils.setField(wishlist, "id", id);
        return wishlist;
    }
}
