package com.ktb.lookddak.domain.fitting.service;

import com.ktb.lookddak.domain.fitting.dto.FittingJobCreateRequest;
import com.ktb.lookddak.domain.fitting.dto.FittingJobCreateResponse;
import com.ktb.lookddak.domain.fitting.entity.FittingJob;
import com.ktb.lookddak.domain.fitting.entity.FittingJobProduct;
import com.ktb.lookddak.domain.fitting.entity.FittingJobStatus;
import com.ktb.lookddak.domain.fitting.repository.FittingCandidateRepository;
import com.ktb.lookddak.domain.fitting.repository.FittingJobProductRepository;
import com.ktb.lookddak.domain.fitting.repository.FittingJobRepository;
import com.ktb.lookddak.domain.fitting.repository.FittingTempResultRepository;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FittingJobServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private FittingCandidateRepository fittingCandidateRepository;

    @Mock
    private FittingJobRepository fittingJobRepository;

    @Mock
    private FittingJobProductRepository fittingJobProductRepository;

    @Mock
    private FittingTempResultRepository fittingTempResultRepository;

    private FittingJobService fittingJobService;

    @BeforeEach
    void setUp() {
        fittingJobService = new FittingJobService(
                memberRepository,
                productRepository,
                fittingCandidateRepository,
                fittingJobRepository,
                fittingJobProductRepository,
                fittingTempResultRepository
        );
    }

    @Test
    @DisplayName("상의와 하의를 선택해 생성 중인 가상피팅 작업을 만든다")
    void createFittingJob() {
        Member member = createMember(1L);
        Product top = createProduct(10L, ProductItemType.TOP);
        Product bottom = createProduct(20L, ProductItemType.BOTTOM);
        given(memberRepository.findActiveByIdForUpdate(1L))
                .willReturn(Optional.of(member));
        given(fittingJobRepository.existsByMemberIdAndStatus(
                1L,
                FittingJobStatus.GENERATING
        )).willReturn(false);
        given(productRepository.findAllById(Set.of(10L, 20L)))
                .willReturn(List.of(bottom, top));
        given(fittingCandidateRepository
                .findProductIdsByMemberIdAndProductIdIn(
                        1L,
                        Set.of(10L, 20L)
                )).willReturn(Set.of(10L, 20L));
        given(fittingJobRepository.save(any(FittingJob.class)))
                .willAnswer(invocation -> {
                    FittingJob fittingJob = invocation.getArgument(0);
                    ReflectionTestUtils.setField(fittingJob, "id", 100L);
                    return fittingJob;
                });

        FittingJobCreateResponse response = fittingJobService
                .createFittingJob(
                        1L,
                        new FittingJobCreateRequest(10L, 20L)
                );

        assertThat(response.getFittingJobId()).isEqualTo(100L);
        assertThat(response.getStatus())
                .isEqualTo(FittingJobStatus.GENERATING);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<FittingJobProduct>> captor =
                ArgumentCaptor.forClass(Iterable.class);
        verify(fittingJobProductRepository).saveAll(captor.capture());

        List<FittingJobProduct> savedProducts = new ArrayList<>();
        captor.getValue().forEach(savedProducts::add);
        assertThat(savedProducts)
                .extracting(jobProduct -> jobProduct.getProduct().getId())
                .containsExactly(10L, 20L);
    }

    @Test
    @DisplayName("상의만 선택해도 가상피팅 작업을 생성할 수 있다")
    void createFittingJobWithTopOnly() {
        Member member = createMember(1L);
        Product top = createProduct(10L, ProductItemType.TOP);
        given(memberRepository.findActiveByIdForUpdate(1L))
                .willReturn(Optional.of(member));
        given(fittingJobRepository.existsByMemberIdAndStatus(
                1L,
                FittingJobStatus.GENERATING
        )).willReturn(false);
        given(productRepository.findAllById(Set.of(10L)))
                .willReturn(List.of(top));
        given(fittingCandidateRepository
                .findProductIdsByMemberIdAndProductIdIn(1L, Set.of(10L)))
                .willReturn(Set.of(10L));
        given(fittingJobRepository.save(any(FittingJob.class)))
                .willAnswer(invocation -> {
                    FittingJob fittingJob = invocation.getArgument(0);
                    ReflectionTestUtils.setField(fittingJob, "id", 100L);
                    return fittingJob;
                });

        FittingJobCreateResponse response = fittingJobService
                .createFittingJob(
                        1L,
                        new FittingJobCreateRequest(10L, null)
                );

        assertThat(response.getFittingJobId()).isEqualTo(100L);
        verify(fittingJobProductRepository).saveAll(any());
    }

    @Test
    @DisplayName("생성 중인 작업이 있으면 새로운 작업을 생성할 수 없다")
    void rejectWhenJobIsGenerating() {
        given(memberRepository.findActiveByIdForUpdate(1L))
                .willReturn(Optional.of(createMember(1L)));
        given(fittingJobRepository.existsByMemberIdAndStatus(
                1L,
                FittingJobStatus.GENERATING
        )).willReturn(true);

        assertBusinessException(
                () -> fittingJobService.createFittingJob(
                        1L,
                        new FittingJobCreateRequest(10L, null)
                ),
                ErrorCode.FITTING_JOB_ALREADY_GENERATING
        );

        verify(productRepository, never()).findAllById(any());
        verify(fittingJobRepository, never()).save(any());
    }

    @Test
    @DisplayName("상의와 하의를 모두 선택하지 않으면 작업을 생성할 수 없다")
    void rejectEmptyProductSelection() {
        given(memberRepository.findActiveByIdForUpdate(1L))
                .willReturn(Optional.of(createMember(1L)));
        given(fittingJobRepository.existsByMemberIdAndStatus(
                1L,
                FittingJobStatus.GENERATING
        )).willReturn(false);

        assertBusinessException(
                () -> fittingJobService.createFittingJob(
                        1L,
                        new FittingJobCreateRequest(null, null)
                ),
                ErrorCode.FITTING_PRODUCT_REQUIRED
        );

        verify(productRepository, never()).findAllById(any());
    }

    @Test
    @DisplayName("선택한 상품이 존재하지 않으면 작업을 생성할 수 없다")
    void rejectMissingProduct() {
        givenReadyMember();
        given(productRepository.findAllById(Set.of(10L)))
                .willReturn(List.of());

        assertBusinessException(
                () -> fittingJobService.createFittingJob(
                        1L,
                        new FittingJobCreateRequest(10L, null)
                ),
                ErrorCode.PRODUCT_NOT_FOUND
        );

        verify(fittingJobRepository, never()).save(any());
    }

    @Test
    @DisplayName("상의 위치에 하의 상품을 선택하면 작업을 생성할 수 없다")
    void rejectProductTypeMismatch() {
        givenReadyMember();
        Product bottom = createProduct(10L, ProductItemType.BOTTOM);
        given(productRepository.findAllById(Set.of(10L)))
                .willReturn(List.of(bottom));

        assertBusinessException(
                () -> fittingJobService.createFittingJob(
                        1L,
                        new FittingJobCreateRequest(10L, null)
                ),
                ErrorCode.FITTING_PRODUCT_TYPE_MISMATCH
        );

        verify(fittingCandidateRepository, never())
                .findProductIdsByMemberIdAndProductIdIn(any(), any());
    }

    @Test
    @DisplayName("피팅 후보에 없는 상품은 작업에 사용할 수 없다")
    void rejectProductNotInFittingCandidates() {
        givenReadyMember();
        Product top = createProduct(10L, ProductItemType.TOP);
        given(productRepository.findAllById(Set.of(10L)))
                .willReturn(List.of(top));
        given(fittingCandidateRepository
                .findProductIdsByMemberIdAndProductIdIn(1L, Set.of(10L)))
                .willReturn(Set.of());

        assertBusinessException(
                () -> fittingJobService.createFittingJob(
                        1L,
                        new FittingJobCreateRequest(10L, null)
                ),
                ErrorCode.FITTING_PRODUCT_NOT_CANDIDATE
        );

        verify(fittingJobRepository, never()).save(any());
    }

    @Test
    @DisplayName("탈퇴했거나 존재하지 않는 회원은 작업을 생성할 수 없다")
    void rejectInactiveMember() {
        given(memberRepository.findActiveByIdForUpdate(1L))
                .willReturn(Optional.empty());

        assertBusinessException(
                () -> fittingJobService.createFittingJob(
                        1L,
                        new FittingJobCreateRequest(10L, null)
                ),
                ErrorCode.RESOURCE_NOT_FOUND
        );

        verify(fittingJobRepository, never())
                .existsByMemberIdAndStatus(any(), any());
    }

    private void givenReadyMember() {
        given(memberRepository.findActiveByIdForUpdate(1L))
                .willReturn(Optional.of(createMember(1L)));
        given(fittingJobRepository.existsByMemberIdAndStatus(
                1L,
                FittingJobStatus.GENERATING
        )).willReturn(false);
    }

    private Member createMember(Long id) {
        Member member = Member.create(
                "fitting-job-service@lookddak.com",
                "encoded-password"
        );
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private Product createProduct(Long id, ProductItemType itemType) {
        Product product = Product.create(
                "product-" + id,
                "테스트 상품",
                "https://image.lookddak.com/products/test.jpg",
                49_000,
                "차콜",
                itemType,
                "https://shop.lookddak.com/products/test"
        );
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    private void assertBusinessException(
            Runnable action,
            ErrorCode expectedErrorCode
    ) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(expectedErrorCode)
                );
    }
}
