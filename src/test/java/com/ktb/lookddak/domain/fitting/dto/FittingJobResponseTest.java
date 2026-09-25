package com.ktb.lookddak.domain.fitting.dto;

import com.ktb.lookddak.domain.fitting.entity.FittingJob;
import com.ktb.lookddak.domain.fitting.entity.FittingJobStatus;
import com.ktb.lookddak.domain.fitting.entity.FittingTempResult;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.product.entity.Product;
import com.ktb.lookddak.domain.product.entity.ProductItemType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FittingJobResponseTest {

    @Test
    @DisplayName("생성된 가상피팅 작업의 ID와 GENERATING 상태를 반환한다")
    void createFittingJobResponse() {
        FittingJob fittingJob = createFittingJob(123L);

        FittingJobCreateResponse response =
                FittingJobCreateResponse.from(fittingJob);

        assertThat(response.getFittingJobId()).isEqualTo(123L);
        assertThat(response.getStatus())
                .isEqualTo(FittingJobStatus.GENERATING);
    }

    @Test
    @DisplayName("생성 중이거나 실패한 작업은 결과 없이 상태를 반환한다")
    void createStatusResponseWithoutResult() {
        FittingJob generatingJob = createFittingJob(123L);
        FittingJob failedJob = createFittingJob(124L);
        failedJob.failGeneration();

        FittingJobStatusResponse generatingResponse =
                FittingJobStatusResponse.from(generatingJob, null);
        FittingJobStatusResponse failedResponse =
                FittingJobStatusResponse.from(failedJob, null);

        assertThat(generatingResponse.getStatus())
                .isEqualTo(FittingJobStatus.GENERATING);
        assertThat(generatingResponse.getResult()).isNull();
        assertThat(failedResponse.getStatus())
                .isEqualTo(FittingJobStatus.FAILED);
        assertThat(failedResponse.getResult()).isNull();
    }

    @Test
    @DisplayName("완료된 작업은 임시 결과와 사용 상품을 반환한다")
    void createCompletedStatusResponse() {
        FittingJob fittingJob = createFittingJob(123L);
        fittingJob.completeGeneration();
        FittingTempResult tempResult = FittingTempResult.create(
                fittingJob,
                "https://image.lookddak.com/fittings/123.jpg",
                "가을 출근 니트 룩",
                "선택한 상하의 조합이 자연스럽게 어우러져 있어요."
        );
        FittingResultProductResponse productResponse =
                FittingResultProductResponse.from(createProduct(1L));
        List<FittingResultProductResponse> products = new ArrayList<>();
        products.add(productResponse);
        FittingResultResponse resultResponse = FittingResultResponse.from(
                tempResult,
                products
        );
        products.clear();

        FittingJobStatusResponse response =
                FittingJobStatusResponse.from(fittingJob, resultResponse);

        assertThat(response.getFittingJobId()).isEqualTo(123L);
        assertThat(response.getStatus())
                .isEqualTo(FittingJobStatus.COMPLETED);
        assertThat(response.getResult().getResultImageUrl())
                .isEqualTo("https://image.lookddak.com/fittings/123.jpg");
        assertThat(response.getResult().getOutfitName())
                .isEqualTo("가을 출근 니트 룩");
        assertThat(response.getResult().getComment())
                .isEqualTo("선택한 상하의 조합이 자연스럽게 어우러져 있어요.");
        assertThat(response.getResult().getProducts())
                .containsExactly(productResponse);
    }

    @Test
    @DisplayName("상태 조회 응답을 API 명세의 필드 순서로 직렬화한다")
    void serializeStatusResponseInSpecifiedOrder() throws Exception {
        FittingJob fittingJob = createFittingJob(123L);
        fittingJob.completeGeneration();
        FittingResultResponse resultResponse = FittingResultResponse.from(
                FittingTempResult.create(
                        fittingJob,
                        "https://image.lookddak.com/fittings/123.jpg",
                        "가을 출근 니트 룩",
                        "자연스러운 조합이에요."
                ),
                List.of(FittingResultProductResponse.from(createProduct(1L)))
        );
        FittingJobStatusResponse response =
                FittingJobStatusResponse.from(fittingJob, resultResponse);
        ObjectMapper objectMapper = JsonMapper.builder()
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .build();

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).containsSubsequence(
                "\"fittingJobId\"",
                "\"status\"",
                "\"result\"",
                "\"resultImageUrl\"",
                "\"outfitName\"",
                "\"comment\"",
                "\"products\"",
                "\"productId\"",
                "\"itemType\"",
                "\"productName\"",
                "\"productImageUrl\"",
                "\"purchaseUrl\""
        );
        assertThat(json).doesNotContain("expiresAt", "failureReason");
    }

    private FittingJob createFittingJob(Long fittingJobId) {
        Member member = Member.create(
                "member@lookddak.com",
                "encoded-password"
        );
        FittingJob fittingJob = FittingJob.create(member);
        ReflectionTestUtils.setField(fittingJob, "id", fittingJobId);
        return fittingJob;
    }

    private Product createProduct(Long productId) {
        Product product = Product.create(
                "product-" + productId,
                "에센셜 램스울 크루넥",
                "https://image.lookddak.com/products/1.jpg",
                49_000,
                "차콜",
                ProductItemType.TOP,
                "https://shop.lookddak.com/products/1"
        );
        ReflectionTestUtils.setField(product, "id", productId);
        return product;
    }
}
