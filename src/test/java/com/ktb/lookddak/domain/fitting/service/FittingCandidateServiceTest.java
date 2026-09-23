package com.ktb.lookddak.domain.fitting.service;

import com.ktb.lookddak.domain.fitting.dto.FittingCandidateCreateRequest;
import com.ktb.lookddak.domain.fitting.dto.FittingCandidateCreateResponse;
import com.ktb.lookddak.domain.fitting.dto.FittingCandidateListResponse;
import com.ktb.lookddak.domain.fitting.entity.FittingCandidate;
import com.ktb.lookddak.domain.fitting.repository.FittingCandidateRepository;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.member.repository.MemberRepository;
import com.ktb.lookddak.domain.product.entity.Product;
import com.ktb.lookddak.domain.product.entity.ProductItemType;
import com.ktb.lookddak.domain.product.repository.ProductRepository;
import com.ktb.lookddak.global.exception.BusinessException;
import com.ktb.lookddak.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class FittingCandidateServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private FittingCandidateRepository fittingCandidateRepository;

    private FittingCandidateService fittingCandidateService;

    @BeforeEach
    void setUp() {
        fittingCandidateService = new FittingCandidateService(
                memberRepository,
                productRepository,
                fittingCandidateRepository
        );
    }

    @Test
    @DisplayName("피팅 후보가 999개이면 상품을 추가할 수 있다")
    void createFittingCandidateAt999() {
        Member member = createMember(1L);
        Product product = createProduct(10L);
        given(memberRepository.findActiveByIdForUpdate(1L))
                .willReturn(Optional.of(member));
        given(productRepository.findById(10L)).willReturn(Optional.of(product));
        given(fittingCandidateRepository.existsByMemberIdAndProductId(1L, 10L))
                .willReturn(false);
        given(fittingCandidateRepository.countByMemberId(1L)).willReturn(999L);
        given(fittingCandidateRepository.saveAndFlush(any(FittingCandidate.class)))
                .willAnswer(invocation -> {
                    FittingCandidate candidate = invocation.getArgument(0);
                    ReflectionTestUtils.setField(candidate, "id", 25L);
                    return candidate;
                });

        FittingCandidateCreateResponse response =
                fittingCandidateService.createFittingCandidate(
                        1L,
                        new FittingCandidateCreateRequest(10L)
                );

        assertThat(response.getFittingCandidateId()).isEqualTo(25L);
        assertThat(response.getProductId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("피팅 후보가 1000개이면 상품을 추가할 수 없다")
    void rejectCandidateLimit() {
        Member member = createMember(1L);
        Product product = createProduct(10L);
        given(memberRepository.findActiveByIdForUpdate(1L))
                .willReturn(Optional.of(member));
        given(productRepository.findById(10L)).willReturn(Optional.of(product));
        given(fittingCandidateRepository.existsByMemberIdAndProductId(1L, 10L))
                .willReturn(false);
        given(fittingCandidateRepository.countByMemberId(1L)).willReturn(1_000L);

        assertThatThrownBy(() -> fittingCandidateService.createFittingCandidate(
                1L,
                new FittingCandidateCreateRequest(10L)
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(
                        ErrorCode.FITTING_CANDIDATE_LIMIT_EXCEEDED
                )
        );

        verify(fittingCandidateRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("1000개를 보유해도 중복 상품 오류를 먼저 반환한다")
    void rejectDuplicateBeforeLimit() {
        Member member = createMember(1L);
        Product product = createProduct(10L);
        given(memberRepository.findActiveByIdForUpdate(1L))
                .willReturn(Optional.of(member));
        given(productRepository.findById(10L)).willReturn(Optional.of(product));
        given(fittingCandidateRepository.existsByMemberIdAndProductId(1L, 10L))
                .willReturn(true);

        assertThatThrownBy(() -> fittingCandidateService.createFittingCandidate(
                1L,
                new FittingCandidateCreateRequest(10L)
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(
                        ErrorCode.FITTING_CANDIDATE_ALREADY_EXISTS
                )
        );

        verify(fittingCandidateRepository, never()).countByMemberId(any());
    }

    @Test
    @DisplayName("존재하지 않는 상품은 후보에 추가할 수 없다")
    void rejectMissingProduct() {
        given(memberRepository.findActiveByIdForUpdate(1L))
                .willReturn(Optional.of(createMember(1L)));
        given(productRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> fittingCandidateService.createFittingCandidate(
                1L,
                new FittingCandidateCreateRequest(10L)
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND)
        );
    }

    @Test
    @DisplayName("UNIQUE 제약 충돌을 중복 후보 오류로 변환한다")
    void handleConcurrentDuplicate() {
        Member member = createMember(1L);
        Product product = createProduct(10L);
        given(memberRepository.findActiveByIdForUpdate(1L))
                .willReturn(Optional.of(member));
        given(productRepository.findById(10L)).willReturn(Optional.of(product));
        given(fittingCandidateRepository.existsByMemberIdAndProductId(1L, 10L))
                .willReturn(false);
        given(fittingCandidateRepository.countByMemberId(1L)).willReturn(0L);
        given(fittingCandidateRepository.saveAndFlush(any(FittingCandidate.class)))
                .willThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> fittingCandidateService.createFittingCandidate(
                1L,
                new FittingCandidateCreateRequest(10L)
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(
                        ErrorCode.FITTING_CANDIDATE_ALREADY_EXISTS
                )
        );
    }

    @Test
    @DisplayName("본인의 피팅 후보를 하드 삭제한다")
    void deleteFittingCandidate() {
        FittingCandidate candidate = createCandidate(25L, createMember(1L));
        given(fittingCandidateRepository.findByIdForUpdate(25L))
                .willReturn(Optional.of(candidate));

        fittingCandidateService.deleteFittingCandidate(1L, 25L);

        verify(fittingCandidateRepository).delete(candidate);
    }

    @Test
    @DisplayName("다른 회원의 피팅 후보는 삭제할 수 없다")
    void rejectOtherMembersCandidate() {
        FittingCandidate candidate = createCandidate(25L, createMember(2L));
        given(fittingCandidateRepository.findByIdForUpdate(25L))
                .willReturn(Optional.of(candidate));

        assertThatThrownBy(() ->
                fittingCandidateService.deleteFittingCandidate(1L, 25L)
        ).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(
                        ErrorCode.FITTING_CANDIDATE_ACCESS_DENIED
                )
        );

        verify(fittingCandidateRepository, never()).delete(any());
    }

    @Test
    @DisplayName("존재하지 않는 피팅 후보는 삭제할 수 없다")
    void rejectMissingCandidate() {
        given(fittingCandidateRepository.findByIdForUpdate(25L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                fittingCandidateService.deleteFittingCandidate(1L, 25L)
        ).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(
                        ErrorCode.FITTING_CANDIDATE_NOT_FOUND
                )
        );
    }

    @Test
    @DisplayName("전체 첫 페이지에 다음 데이터가 있으면 size개와 Cursor를 반환한다")
    void getFirstPageWithNextPage() {
        Member member = createMember(1L);
        FittingCandidate first = createCandidate(30L, member);
        FittingCandidate second = createCandidate(29L, member);
        FittingCandidate nextPageCandidate = createCandidate(28L, member);
        given(fittingCandidateRepository.findFirstPage(
                eq(1L),
                any(Pageable.class)
        )).willReturn(List.of(first, second, nextPageCandidate));

        FittingCandidateListResponse response = fittingCandidateService
                .getFittingCandidates(1L, null, null, 2);

        assertThat(response.getItems())
                .extracting(item -> item.getFittingCandidateId())
                .containsExactly(30L, 29L);
        assertThat(response.getNextCursor()).isEqualTo(29L);
        assertThat(response.isHasNext()).isTrue();
        verify(fittingCandidateRepository).findFirstPage(
                eq(1L),
                argThat(pageable -> pageable.getPageSize() == 3)
        );
    }

    @Test
    @DisplayName("전체 다음 페이지가 마지막이면 null Cursor를 반환한다")
    void getLastNextPage() {
        FittingCandidate candidate = createCandidate(
                25L,
                createMember(1L)
        );
        given(fittingCandidateRepository.findNextPage(
                eq(1L),
                eq(29L),
                any(Pageable.class)
        )).willReturn(List.of(candidate));

        FittingCandidateListResponse response = fittingCandidateService
                .getFittingCandidates(1L, null, 29L, 20);

        assertThat(response.getItems())
                .extracting(item -> item.getFittingCandidateId())
                .containsExactly(25L);
        assertThat(response.getNextCursor()).isNull();
        assertThat(response.isHasNext()).isFalse();
    }

    @Test
    @DisplayName("상품 타입이 있으면 타입별 첫 페이지를 조회한다")
    void getFirstPageByItemType() {
        FittingCandidate topCandidate = createCandidate(
                30L,
                createMember(1L),
                ProductItemType.TOP
        );
        given(fittingCandidateRepository.findFirstPageByItemType(
                eq(1L),
                eq(ProductItemType.TOP),
                any(Pageable.class)
        )).willReturn(List.of(topCandidate));

        FittingCandidateListResponse response = fittingCandidateService
                .getFittingCandidates(
                        1L,
                        ProductItemType.TOP,
                        null,
                        20
                );

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getItemType())
                .isEqualTo(ProductItemType.TOP);
    }

    @Test
    @DisplayName("상품 타입과 Cursor가 있으면 타입별 다음 페이지를 조회한다")
    void getNextPageByItemType() {
        FittingCandidate bottomCandidate = createCandidate(
                25L,
                createMember(1L),
                ProductItemType.BOTTOM
        );
        given(fittingCandidateRepository.findNextPageByItemType(
                eq(1L),
                eq(29L),
                eq(ProductItemType.BOTTOM),
                any(Pageable.class)
        )).willReturn(List.of(bottomCandidate));

        FittingCandidateListResponse response = fittingCandidateService
                .getFittingCandidates(
                        1L,
                        ProductItemType.BOTTOM,
                        29L,
                        20
                );

        assertThat(response.getItems())
                .extracting(item -> item.getFittingCandidateId())
                .containsExactly(25L);
        assertThat(response.getItems().get(0).getItemType())
                .isEqualTo(ProductItemType.BOTTOM);
    }

    @Test
    @DisplayName("size를 생략하면 20개보다 한 건 더 조회한다")
    void getEmptyPageWithDefaultSize() {
        given(fittingCandidateRepository.findFirstPage(
                eq(1L),
                any(Pageable.class)
        )).willReturn(List.of());

        FittingCandidateListResponse response = fittingCandidateService
                .getFittingCandidates(1L, null, null, null);

        assertThat(response.getItems()).isEmpty();
        assertThat(response.getNextCursor()).isNull();
        assertThat(response.isHasNext()).isFalse();
        verify(fittingCandidateRepository).findFirstPage(
                eq(1L),
                argThat(pageable -> pageable.getPageSize() == 21)
        );
    }

    @Test
    @DisplayName("Cursor나 조회 크기가 범위를 벗어나면 목록 조회를 거부한다")
    void rejectInvalidPaginationRange() {
        assertPaginationError(() -> fittingCandidateService
                .getFittingCandidates(1L, null, 0L, 20));
        assertPaginationError(() -> fittingCandidateService
                .getFittingCandidates(1L, null, -1L, 20));
        assertPaginationError(() -> fittingCandidateService
                .getFittingCandidates(1L, null, null, 0));
        assertPaginationError(() -> fittingCandidateService
                .getFittingCandidates(1L, null, null, 101));

        verifyNoInteractions(fittingCandidateRepository);
    }

    private void assertPaginationError(
            org.assertj.core.api.ThrowableAssert.ThrowingCallable callable
    ) {
        assertThatThrownBy(callable)
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(
                                ErrorCode.INVALID_PAGINATION_PARAMETER
                        )
                );
    }

    private Member createMember(Long id) {
        Member member = Member.create("member" + id + "@lookddak.com", "encoded");
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private Product createProduct(Long id) {
        return createProduct(id, ProductItemType.TOP);
    }

    private Product createProduct(Long id, ProductItemType itemType) {
        Product product = Product.create(
                "테스트 상품",
                "https://image.lookddak.com/test.jpg",
                49_000,
                "네이비",
                itemType,
                "https://shop.lookddak.com/test"
        );
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    private FittingCandidate createCandidate(Long id, Member member) {
        return createCandidate(id, member, ProductItemType.TOP);
    }

    private FittingCandidate createCandidate(
            Long id,
            Member member,
            ProductItemType itemType
    ) {
        FittingCandidate candidate = FittingCandidate.create(
                member,
                createProduct(10L, itemType)
        );
        ReflectionTestUtils.setField(candidate, "id", id);
        return candidate;
    }
}
