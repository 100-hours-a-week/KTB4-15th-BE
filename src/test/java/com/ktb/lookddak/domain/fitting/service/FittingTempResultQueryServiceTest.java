package com.ktb.lookddak.domain.fitting.service;

import com.ktb.lookddak.domain.fitting.dto.FittingTempResultDetailResponse;
import com.ktb.lookddak.domain.fitting.entity.FittingJob;
import com.ktb.lookddak.domain.fitting.entity.FittingJobProduct;
import com.ktb.lookddak.domain.fitting.entity.FittingResult;
import com.ktb.lookddak.domain.fitting.entity.FittingTempResult;
import com.ktb.lookddak.domain.fitting.repository.FittingJobProductRepository;
import com.ktb.lookddak.domain.fitting.repository.FittingJobRepository;
import com.ktb.lookddak.domain.fitting.repository.FittingResultRepository;
import com.ktb.lookddak.domain.fitting.repository.FittingTempResultRepository;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.product.entity.Product;
import com.ktb.lookddak.domain.product.entity.ProductItemType;
import com.ktb.lookddak.global.exception.BusinessException;
import com.ktb.lookddak.global.exception.ErrorCode;
import com.ktb.lookddak.global.storage.s3.S3PresignedUrlProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FittingTempResultQueryServiceTest {

    @Mock
    private FittingJobRepository fittingJobRepository;

    @Mock
    private FittingTempResultRepository fittingTempResultRepository;

    @Mock
    private FittingJobProductRepository fittingJobProductRepository;

    @Mock
    private FittingResultRepository fittingResultRepository;

    @Mock
    private S3PresignedUrlProvider presignedUrlProvider;

    private FittingTempResultQueryService service;

    @BeforeEach
    void setUp() {
        service = new FittingTempResultQueryService(
                fittingJobRepository,
                fittingTempResultRepository,
                fittingJobProductRepository,
                fittingResultRepository,
                presignedUrlProvider
        );
    }

    @Test
    @DisplayName("완료된 임시 가상피팅 결과와 영구 저장 상태를 조회한다")
    void getTempResult() {
        FittingJob fittingJob = completedJob(100L, 1L);
        FittingTempResult tempResult = tempResult(fittingJob);
        FittingResult fittingResult = FittingResult.create(
                fittingJob,
                "fittings/result.png",
                "저장 코디명",
                "코디 설명"
        );
        ReflectionTestUtils.setField(fittingResult, "id", 200L);
        given(fittingJobRepository.findByIdWithMember(100L))
                .willReturn(Optional.of(fittingJob));
        given(fittingTempResultRepository.findByFittingJobId(100L))
                .willReturn(Optional.of(tempResult));
        given(fittingJobProductRepository
                .findAllByFittingJobIdWithProduct(100L))
                .willReturn(List.of(FittingJobProduct.create(
                        fittingJob,
                        product(10L)
                )));
        given(fittingResultRepository.findByFittingJobId(100L))
                .willReturn(Optional.of(fittingResult));
        given(presignedUrlProvider.createGetUrl("fittings/result.png"))
                .willReturn("https://signed.example.com/result.png");

        FittingTempResultDetailResponse response = service
                .getTempResult(1L, 100L);

        assertThat(response.getFittingJobId()).isEqualTo(100L);
        assertThat(response.isSaved()).isTrue();
        assertThat(response.getFittingResultId()).isEqualTo(200L);
        assertThat(response.getOutfitName()).isEqualTo("AI 코디명");
        assertThat(response.getProducts()).singleElement().satisfies(item ->
                assertThat(item.getProductId()).isEqualTo(10L)
        );
    }

    @Test
    @DisplayName("생성 실패한 작업의 임시 결과는 조회할 수 없다")
    void rejectFailedJob() {
        FittingJob fittingJob = job(100L, 1L);
        fittingJob.failGeneration();
        given(fittingJobRepository.findByIdWithMember(100L))
                .willReturn(Optional.of(fittingJob));

        assertThatThrownBy(() -> service.getTempResult(1L, 100L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FITTING_JOB_FAILED);
    }

    @Test
    @DisplayName("만료된 임시 결과는 조회할 수 없다")
    void rejectExpiredTempResult() {
        FittingJob fittingJob = completedJob(100L, 1L);
        FittingTempResult tempResult = tempResult(fittingJob);
        ReflectionTestUtils.setField(
                tempResult,
                "expiresAt",
                LocalDateTime.now().minusSeconds(1)
        );
        given(fittingJobRepository.findByIdWithMember(100L))
                .willReturn(Optional.of(fittingJob));
        given(fittingTempResultRepository.findByFittingJobId(100L))
                .willReturn(Optional.of(tempResult));

        assertThatThrownBy(() -> service.getTempResult(1L, 100L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FITTING_TEMP_RESULT_NOT_FOUND);
    }

    private FittingJob completedJob(Long fittingJobId, Long memberId) {
        FittingJob fittingJob = job(fittingJobId, memberId);
        fittingJob.completeGeneration();
        return fittingJob;
    }

    private FittingJob job(Long fittingJobId, Long memberId) {
        Member member = Member.create(
                "member" + memberId + "@lookddak.com",
                "password"
        );
        ReflectionTestUtils.setField(member, "id", memberId);
        FittingJob fittingJob = FittingJob.create(member);
        ReflectionTestUtils.setField(fittingJob, "id", fittingJobId);
        return fittingJob;
    }

    private FittingTempResult tempResult(FittingJob fittingJob) {
        return FittingTempResult.create(
                fittingJob,
                "fittings/result.png",
                "AI 코디명",
                "코디 설명"
        );
    }

    private Product product(Long productId) {
        Product product = Product.create(
                "product-code",
                "상품명",
                "https://image.example.com/product.jpg",
                10000,
                "NAVY",
                ProductItemType.TOP,
                "https://shop.example.com/product"
        );
        ReflectionTestUtils.setField(product, "id", productId);
        return product;
    }
}
