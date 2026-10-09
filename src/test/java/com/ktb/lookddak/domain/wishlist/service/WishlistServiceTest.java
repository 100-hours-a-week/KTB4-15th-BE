package com.ktb.lookddak.domain.wishlist.service;

import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.member.repository.MemberRepository;
import com.ktb.lookddak.domain.product.entity.Product;
import com.ktb.lookddak.domain.product.entity.ProductItemType;
import com.ktb.lookddak.domain.product.repository.ProductRepository;
import com.ktb.lookddak.domain.wishlist.dto.WishlistCreateRequest;
import com.ktb.lookddak.domain.wishlist.dto.WishlistCreateResponse;
import com.ktb.lookddak.domain.wishlist.dto.WishlistCountResponse;
import com.ktb.lookddak.domain.wishlist.dto.WishlistListResponse;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    @DisplayName("찜 목록을 최신 찜 순으로 조회하고 가격 변동률과 다음 커서를 반환한다")
    void getWishlists() {
        Wishlist newestWishlist = createWishlist(30L, createMember(1L));
        Wishlist middleWishlist = createWishlist(20L, createMember(1L));
        Wishlist nextWishlist = createWishlist(10L, createMember(1L));
        ReflectionTestUtils.setField(
                newestWishlist.getProduct(), "currentPrice", 40_000
        );
        ReflectionTestUtils.setField(
                middleWishlist.getProduct(), "currentPrice", 55_000
        );

        given(wishlistRepository.findFirstPage(eq(1L), any()))
                .willReturn(List.of(
                        newestWishlist,
                        middleWishlist,
                        nextWishlist
                ));
        given(wishlistRepository.countByMemberId(1L)).willReturn(3L);

        WishlistListResponse response = wishlistService.getWishlists(
                1L,
                null,
                2
        );

        assertThat(response.getTotalCount()).isEqualTo(3L);
        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getItems().get(0).getWishlistId()).isEqualTo(30L);
        assertThat(response.getItems().get(0).getPriceChangeRate())
                .isEqualTo(-18);
        assertThat(response.getItems().get(1).getPriceChangeRate())
                .isEqualTo(12);
        assertThat(response.getNextCursor()).isEqualTo(20L);
        assertThat(response.isHasNext()).isTrue();
    }

    @Test
    @DisplayName("다음 찜 목록을 커서 기준으로 조회한다")
    void getNextWishlists() {
        Wishlist wishlist = createWishlist(10L, createMember(1L));
        given(wishlistRepository.findNextPage(eq(1L), eq(20L), any()))
                .willReturn(List.of(wishlist));
        given(wishlistRepository.countByMemberId(1L)).willReturn(3L);

        WishlistListResponse response = wishlistService.getWishlists(
                1L,
                20L,
                20
        );

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getNextCursor()).isNull();
        assertThat(response.isHasNext()).isFalse();
    }

    @Test
    @DisplayName("찜 목록이 없으면 빈 목록과 전체 개수 0을 반환한다")
    void getEmptyWishlists() {
        given(wishlistRepository.findFirstPage(eq(1L), any()))
                .willReturn(List.of());
        given(wishlistRepository.countByMemberId(1L)).willReturn(0L);

        WishlistListResponse response = wishlistService.getWishlists(
                1L,
                null,
                null
        );

        assertThat(response.getTotalCount()).isZero();
        assertThat(response.getItems()).isEmpty();
        assertThat(response.getNextCursor()).isNull();
        assertThat(response.isHasNext()).isFalse();
    }

    @Test
    @DisplayName("유효하지 않은 커서 또는 크기는 조회할 수 없다")
    void rejectInvalidPagination() {
        assertThatThrownBy(() -> wishlistService.getWishlists(
                1L,
                0L,
                20
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_PAGINATION_PARAMETER)
        );

        assertThatThrownBy(() -> wishlistService.getWishlists(
                1L,
                null,
                101
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_PAGINATION_PARAMETER)
        );
    }

    @Test
    @DisplayName("상품을 찜하고 현재 가격을 저장한다")
    void createWishlist() {
        Member member = createMember(1L);
        Product product = createProduct(10L, 49_000);
        given(memberRepository.findActiveByIdForUpdate(1L))
                .willReturn(Optional.of(member));
        given(wishlistRepository.countByMemberId(1L)).willReturn(0L);
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
        given(memberRepository.findActiveByIdForUpdate(1L))
                .willReturn(Optional.of(createMember(1L)));
        given(wishlistRepository.countByMemberId(1L)).willReturn(0L);
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
        given(memberRepository.findActiveByIdForUpdate(1L))
                .willReturn(Optional.of(member));
        given(wishlistRepository.countByMemberId(1L)).willReturn(0L);
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
        given(memberRepository.findActiveByIdForUpdate(1L))
                .willReturn(Optional.of(member));
        given(wishlistRepository.countByMemberId(1L)).willReturn(0L);
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
    @DisplayName("찜이 300개이면 추가할 수 없다")
    void rejectWishlistOverLimit() {
        given(memberRepository.findActiveByIdForUpdate(1L))
                .willReturn(Optional.of(createMember(1L)));
        given(wishlistRepository.countByMemberId(1L)).willReturn(300L);

        assertThatThrownBy(() -> wishlistService.createWishlist(
                1L,
                new WishlistCreateRequest(10L)
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.WISHLIST_LIMIT_EXCEEDED)
        );

        verify(productRepository, never()).findById(any());
        verify(wishlistRepository, never()).saveAndFlush(any());
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
                "product-" + id,
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
