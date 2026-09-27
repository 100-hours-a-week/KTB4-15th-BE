package com.ktb.lookddak.domain.fitting;

import com.ktb.lookddak.domain.fitting.dto.FittingJobCreateRequest;
import com.ktb.lookddak.domain.fitting.dto.FittingJobCreateResponse;
import com.ktb.lookddak.domain.fitting.entity.FittingCandidate;
import com.ktb.lookddak.domain.fitting.entity.FittingJob;
import com.ktb.lookddak.domain.fitting.entity.FittingJobStatus;
import com.ktb.lookddak.domain.fitting.entity.FittingTempResult;
import com.ktb.lookddak.domain.fitting.repository.FittingCandidateRepository;
import com.ktb.lookddak.domain.fitting.repository.FittingJobProductRepository;
import com.ktb.lookddak.domain.fitting.repository.FittingJobRepository;
import com.ktb.lookddak.domain.fitting.repository.FittingTempResultRepository;
import com.ktb.lookddak.domain.fitting.service.FittingJobService;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.member.entity.MemberProfile;
import com.ktb.lookddak.domain.member.repository.MemberProfileRepository;
import com.ktb.lookddak.domain.member.repository.MemberRepository;
import com.ktb.lookddak.domain.product.entity.Product;
import com.ktb.lookddak.domain.product.entity.ProductItemType;
import com.ktb.lookddak.domain.product.repository.ProductRepository;
import com.ktb.lookddak.global.client.ai.fitting.AiFittingClient;
import com.ktb.lookddak.global.client.ai.fitting.dto.AiFittingRequest;
import com.ktb.lookddak.global.client.ai.fitting.dto.AiFittingResultData;
import com.ktb.lookddak.global.client.ai.fitting.exception.AiFittingException;
import com.ktb.lookddak.global.storage.s3.S3PresignedUrlProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@SpringBootTest
class FittingAiPipelineIntegrationTest {

    private static final Duration ASYNC_TIMEOUT = Duration.ofSeconds(3);

    @Autowired
    private FittingJobService fittingJobService;

    @Autowired
    private FittingJobRepository fittingJobRepository;

    @Autowired
    private FittingJobProductRepository fittingJobProductRepository;

    @Autowired
    private FittingTempResultRepository fittingTempResultRepository;

    @Autowired
    private FittingCandidateRepository fittingCandidateRepository;

    @Autowired
    private MemberProfileRepository memberProfileRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MemberRepository memberRepository;

    @MockitoBean
    private AiFittingClient aiFittingClient;

    @MockitoBean
    private S3PresignedUrlProvider presignedUrlProvider;

    @BeforeEach
    void cleanUp() {
        fittingTempResultRepository.deleteAll();
        fittingJobProductRepository.deleteAll();
        fittingJobRepository.deleteAll();
        fittingCandidateRepository.deleteAll();
        memberProfileRepository.deleteAll();
        productRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("작업 생성 커밋 후 AI 결과를 비동기로 저장하고 완료 처리한다")
    void completeFittingAfterCommit() {
        TestData data = saveTestData("fitting-pipeline-success@lookddak.com");
        given(presignedUrlProvider.createGetUrl("profiles/member.png"))
                .willReturn("https://presigned.example.com/member.png");
        given(aiFittingClient.requestFitting(any(AiFittingRequest.class)))
                .willReturn(new AiFittingResultData(
                        "fittings/generated-result.png",
                        "차분한 데일리 룩",
                        "깔끔한 색감이 자연스럽게 어우러집니다."
                ));

        FittingJobCreateResponse response = fittingJobService
                .createFittingJob(
                        data.member().getId(),
                        new FittingJobCreateRequest(
                                data.product().getId(),
                                null
                        )
                );

        await(() -> isJobStatus(
                response.getFittingJobId(),
                FittingJobStatus.COMPLETED
        ));

        FittingTempResult result = fittingTempResultRepository
                .findByFittingJobId(response.getFittingJobId())
                .orElseThrow();
        assertThat(result.getResultImageKey())
                .isEqualTo("fittings/generated-result.png");
        assertThat(result.getOutfitName()).isEqualTo("차분한 데일리 룩");
        assertThat(result.getAiComment())
                .isEqualTo("깔끔한 색감이 자연스럽게 어우러집니다.");
    }

    @Test
    @DisplayName("AI 호출이 실패하면 가상피팅 작업을 실패 처리한다")
    void failFittingWhenAiCallFails() {
        TestData data = saveTestData("fitting-pipeline-failure@lookddak.com");
        given(presignedUrlProvider.createGetUrl("profiles/member.png"))
                .willReturn("https://presigned.example.com/member.png");
        given(aiFittingClient.requestFitting(any(AiFittingRequest.class)))
                .willThrow(new AiFittingException(
                        "AI_RESPONSE_TIMEOUT",
                        "AI 가상피팅 응답 시간을 초과했습니다.",
                        null
                ));

        FittingJobCreateResponse response = fittingJobService
                .createFittingJob(
                        data.member().getId(),
                        new FittingJobCreateRequest(
                                data.product().getId(),
                                null
                        )
                );

        await(() -> isJobStatus(
                response.getFittingJobId(),
                FittingJobStatus.FAILED
        ));

        assertThat(fittingTempResultRepository
                .findByFittingJobId(response.getFittingJobId())).isEmpty();
    }

    private TestData saveTestData(String email) {
        Member member = memberRepository.saveAndFlush(Member.create(
                email,
                "encoded-password"
        ));
        memberProfileRepository.saveAndFlush(MemberProfile.create(
                member,
                "김민준",
                29,
                new BigDecimal("175.0"),
                new BigDecimal("70.0"),
                "profiles/member.png"
        ));
        Product product = productRepository.saveAndFlush(Product.create(
                "1234333",
                "검정 니트",
                "https://image.example.com/product.jpg",
                49_000,
                "BLACK",
                ProductItemType.TOP,
                "https://shop.example.com/product"
        ));
        fittingCandidateRepository.saveAndFlush(
                FittingCandidate.create(member, product)
        );
        return new TestData(member, product);
    }

    private boolean isJobStatus(
            Long fittingJobId,
            FittingJobStatus expectedStatus
    ) {
        return fittingJobRepository.findById(fittingJobId)
                .map(FittingJob::getStatus)
                .filter(expectedStatus::equals)
                .isPresent();
    }

    private void await(BooleanSupplier condition) {
        Instant deadline = Instant.now().plus(ASYNC_TIMEOUT);

        while (Instant.now().isBefore(deadline)) {
            if (condition.getAsBoolean()) {
                return;
            }

            try {
                Thread.sleep(20);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AssertionError(
                        "비동기 가상피팅 결과를 기다리는 중 중단되었습니다.",
                        exception
                );
            }
        }

        throw new AssertionError(
                "비동기 가상피팅 결과가 제한 시간 안에 저장되지 않았습니다."
        );
    }

    private static class TestData {

        private final Member member;
        private final Product product;

        private TestData(Member member, Product product) {
            this.member = member;
            this.product = product;
        }

        private Member member() {
            return member;
        }

        private Product product() {
            return product;
        }
    }
}
