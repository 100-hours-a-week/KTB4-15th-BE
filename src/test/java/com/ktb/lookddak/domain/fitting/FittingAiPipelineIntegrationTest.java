package com.ktb.lookddak.domain.fitting;

import com.ktb.lookddak.domain.fitting.dto.FittingJobCreateRequest;
import com.ktb.lookddak.domain.fitting.dto.FittingJobCreateResponse;
import com.ktb.lookddak.domain.fitting.entity.FittingCandidate;
import com.ktb.lookddak.domain.fitting.entity.FittingJob;
import com.ktb.lookddak.domain.fitting.entity.FittingJobStatus;
import com.ktb.lookddak.domain.fitting.entity.FittingTempResult;
import com.ktb.lookddak.domain.fitting.mq.outbox.OutboxEvent;
import com.ktb.lookddak.domain.fitting.mq.outbox.OutboxEventRepository;
import com.ktb.lookddak.domain.fitting.mq.outbox.OutboxEventStatus;
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
import com.ktb.lookddak.global.storage.s3.S3PresignedUrlProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest
class FittingAiPipelineIntegrationTest {

    @Autowired
    private FittingJobService fittingJobService;

    @Autowired
    private FittingJobRepository fittingJobRepository;

    @Autowired
    private FittingJobProductRepository fittingJobProductRepository;

    @Autowired
    private FittingTempResultRepository fittingTempResultRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

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
        outboxEventRepository.deleteAll();
        fittingTempResultRepository.deleteAll();
        fittingJobProductRepository.deleteAll();
        fittingJobRepository.deleteAll();
        fittingCandidateRepository.deleteAll();
        memberProfileRepository.deleteAll();
        productRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("작업 생성 시 PENDING Job과 요청 Outbox 이벤트를 함께 저장한다")
    void savePendingJobAndRequestOutboxTogether() {
        TestData data = saveTestData("fitting-pipeline-success@lookddak.com");

        FittingJobCreateResponse response = fittingJobService
                .createFittingJob(
                        data.member().getId(),
                        new FittingJobCreateRequest(
                                data.product().getId(),
                                null
                        )
                );

        FittingJob fittingJob = fittingJobRepository
                .findById(response.getFittingJobId())
                .orElseThrow();
        OutboxEvent outboxEvent = outboxEventRepository.findAll().getFirst();

        assertThat(fittingJob.getStatus()).isEqualTo(FittingJobStatus.PENDING);
        assertThat(outboxEvent.getAggregateId()).isEqualTo(fittingJob.getId());
        assertThat(outboxEvent.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(outboxEvent.getPayload()).isEqualTo(
                "{\"fitting_job_id\":%d,\"user_image_key\":\"profiles/member.png\","
                        .formatted(fittingJob.getId())
                        + "\"products\":[{\"product_code\":\"1234333\"}]}"
        );
    }

    @Test
    @DisplayName("생성 API는 AI HTTP 호출 없이 PENDING 작업만 접수한다")
    void doNotCallAiHttpWhenCreatingFittingJob() {
        TestData data = saveTestData("fitting-pipeline-failure@lookddak.com");

        FittingJobCreateResponse response = fittingJobService
                .createFittingJob(
                        data.member().getId(),
                        new FittingJobCreateRequest(
                                data.product().getId(),
                                null
                        )
                );

        assertThat(fittingJobRepository.findById(response.getFittingJobId()))
                .map(FittingJob::getStatus)
                .contains(FittingJobStatus.PENDING);
        assertThat(fittingTempResultRepository
                .findByFittingJobId(response.getFittingJobId())).isEmpty();
        verifyNoInteractions(aiFittingClient, presignedUrlProvider);
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
