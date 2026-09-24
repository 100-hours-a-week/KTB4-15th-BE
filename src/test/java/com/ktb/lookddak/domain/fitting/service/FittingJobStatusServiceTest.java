package com.ktb.lookddak.domain.fitting.service;

import com.ktb.lookddak.domain.fitting.dto.FittingJobStatusResponse;
import com.ktb.lookddak.domain.fitting.entity.FittingJob;
import com.ktb.lookddak.domain.fitting.entity.FittingJobProduct;
import com.ktb.lookddak.domain.fitting.entity.FittingJobStatus;
import com.ktb.lookddak.domain.fitting.entity.FittingTempResult;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class FittingJobStatusServiceTest {

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
    @DisplayName("생성 중인 작업은 결과 없이 상태만 반환한다")
    void getGeneratingStatus() {
        FittingJob fittingJob = createFittingJob(100L, 1L);
        given(fittingJobRepository.findByIdWithMember(100L))
                .willReturn(Optional.of(fittingJob));

        FittingJobStatusResponse response = fittingJobService
                .getFittingJobStatus(1L, 100L);

        assertThat(response.getFittingJobId()).isEqualTo(100L);
        assertThat(response.getStatus())
                .isEqualTo(FittingJobStatus.GENERATING);
        assertThat(response.getResult()).isNull();
        verifyNoInteractions(
                fittingTempResultRepository,
                fittingJobProductRepository
        );
    }

    @Test
    @DisplayName("실패한 작업은 결과 없이 상태만 반환한다")
    void getFailedStatus() {
        FittingJob fittingJob = createFittingJob(100L, 1L);
        fittingJob.failGeneration();
        given(fittingJobRepository.findByIdWithMember(100L))
                .willReturn(Optional.of(fittingJob));

        FittingJobStatusResponse response = fittingJobService
                .getFittingJobStatus(1L, 100L);

        assertThat(response.getStatus()).isEqualTo(FittingJobStatus.FAILED);
        assertThat(response.getResult()).isNull();
        verifyNoInteractions(
                fittingTempResultRepository,
                fittingJobProductRepository
        );
    }

    @Test
    @DisplayName("완료된 작업은 결과와 사용 상품을 반환한다")
    void getCompletedStatus() {
        FittingJob fittingJob = createFittingJob(100L, 1L);
        fittingJob.completeGeneration();
        FittingTempResult result = FittingTempResult.create(
                fittingJob,
                "https://image.lookddak.com/fittings/result.jpg",
                "가을 출근 니트 룩",
                "선택한 상하의가 자연스럽게 어우러져 있어요."
        );
        Product top = createProduct(10L, ProductItemType.TOP, "니트");
        Product bottom = createProduct(20L, ProductItemType.BOTTOM, "팬츠");
        given(fittingJobRepository.findByIdWithMember(100L))
                .willReturn(Optional.of(fittingJob));
        given(fittingTempResultRepository.findByFittingJobId(100L))
                .willReturn(Optional.of(result));
        given(fittingJobProductRepository
                .findAllByFittingJobIdWithProduct(100L))
                .willReturn(List.of(
                        FittingJobProduct.create(fittingJob, top),
                        FittingJobProduct.create(fittingJob, bottom)
                ));

        FittingJobStatusResponse response = fittingJobService
                .getFittingJobStatus(1L, 100L);

        assertThat(response.getStatus())
                .isEqualTo(FittingJobStatus.COMPLETED);
        assertThat(response.getResult().getResultImageUrl())
                .isEqualTo(
                        "https://image.lookddak.com/fittings/result.jpg"
                );
        assertThat(response.getResult().getOutfitName())
                .isEqualTo("가을 출근 니트 룩");
        assertThat(response.getResult().getProducts())
                .extracting(product -> product.getProductId())
                .containsExactly(10L, 20L);
    }

    @Test
    @DisplayName("존재하지 않는 작업은 조회할 수 없다")
    void rejectMissingFittingJob() {
        given(fittingJobRepository.findByIdWithMember(100L))
                .willReturn(Optional.empty());

        assertBusinessException(
                () -> fittingJobService.getFittingJobStatus(1L, 100L),
                ErrorCode.FITTING_JOB_NOT_FOUND
        );
    }

    @Test
    @DisplayName("다른 회원의 작업은 조회할 수 없다")
    void rejectOtherMembersFittingJob() {
        FittingJob fittingJob = createFittingJob(100L, 2L);
        given(fittingJobRepository.findByIdWithMember(100L))
                .willReturn(Optional.of(fittingJob));

        assertBusinessException(
                () -> fittingJobService.getFittingJobStatus(1L, 100L),
                ErrorCode.FITTING_JOB_ACCESS_DENIED
        );

        verifyNoInteractions(
                fittingTempResultRepository,
                fittingJobProductRepository
        );
    }

    @Test
    @DisplayName("완료된 작업에 결과가 없으면 서버 데이터 오류로 처리한다")
    void rejectCompletedJobWithoutResult() {
        FittingJob fittingJob = createFittingJob(100L, 1L);
        fittingJob.completeGeneration();
        given(fittingJobRepository.findByIdWithMember(100L))
                .willReturn(Optional.of(fittingJob));
        given(fittingTempResultRepository.findByFittingJobId(100L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                fittingJobService.getFittingJobStatus(1L, 100L)
        ).isInstanceOf(IllegalStateException.class)
                .hasMessage("완료된 가상피팅 작업의 결과가 존재하지 않습니다.");
    }

    @Test
    @DisplayName("완료된 작업에 상품이 없으면 서버 데이터 오류로 처리한다")
    void rejectCompletedJobWithoutProducts() {
        FittingJob fittingJob = createFittingJob(100L, 1L);
        fittingJob.completeGeneration();
        FittingTempResult result = FittingTempResult.create(
                fittingJob,
                "https://image.lookddak.com/fittings/result.jpg",
                "가을 출근 니트 룩",
                "추천 설명"
        );
        given(fittingJobRepository.findByIdWithMember(100L))
                .willReturn(Optional.of(fittingJob));
        given(fittingTempResultRepository.findByFittingJobId(100L))
                .willReturn(Optional.of(result));
        given(fittingJobProductRepository
                .findAllByFittingJobIdWithProduct(100L))
                .willReturn(List.of());

        assertThatThrownBy(() ->
                fittingJobService.getFittingJobStatus(1L, 100L)
        ).isInstanceOf(IllegalStateException.class)
                .hasMessage("완료된 가상피팅 작업의 상품이 존재하지 않습니다.");
    }

    private FittingJob createFittingJob(Long jobId, Long memberId) {
        Member member = Member.create(
                "fitting-status-" + memberId + "@lookddak.com",
                "encoded-password"
        );
        ReflectionTestUtils.setField(member, "id", memberId);
        FittingJob fittingJob = FittingJob.create(member);
        ReflectionTestUtils.setField(fittingJob, "id", jobId);
        return fittingJob;
    }

    private Product createProduct(
            Long productId,
            ProductItemType itemType,
            String name
    ) {
        Product product = Product.create(
                "product-" + productId,
                name,
                "https://image.lookddak.com/products/" + productId + ".jpg",
                49_000,
                "차콜",
                itemType,
                "https://shop.lookddak.com/products/" + productId
        );
        ReflectionTestUtils.setField(product, "id", productId);
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
