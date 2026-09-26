package com.ktb.lookddak.domain.fitting.repository;

import com.ktb.lookddak.domain.fitting.entity.FittingJob;
import com.ktb.lookddak.domain.fitting.entity.FittingJobProduct;
import com.ktb.lookddak.domain.fitting.entity.FittingJobStatus;
import com.ktb.lookddak.domain.fitting.entity.FittingTempResult;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.member.repository.MemberRepository;
import com.ktb.lookddak.domain.product.entity.Product;
import com.ktb.lookddak.domain.product.entity.ProductItemType;
import com.ktb.lookddak.domain.product.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(com.ktb.lookddak.global.config.JpaConfig.class)
class FittingJobRepositoryTest {

    @Autowired
    private FittingJobRepository fittingJobRepository;

    @Autowired
    private FittingJobProductRepository fittingJobProductRepository;

    @Autowired
    private FittingTempResultRepository fittingTempResultRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    private Member member;

    @BeforeEach
    void setUp() {
        member = memberRepository.saveAndFlush(Member.create(
                "fitting-job-repository@lookddak.com",
                "encoded-password"
        ));
    }

    @Test
    @DisplayName("회원의 생성 중인 작업 존재 여부를 확인한다")
    void existsGeneratingJobByMember() {
        FittingJob generatingJob = fittingJobRepository.saveAndFlush(
                FittingJob.create(member)
        );
        FittingJob completedJob = FittingJob.create(member);
        completedJob.completeGeneration();
        fittingJobRepository.saveAndFlush(completedJob);

        assertThat(fittingJobRepository.existsByMemberIdAndStatus(
                member.getId(),
                FittingJobStatus.GENERATING
        )).isTrue();
        assertThat(generatingJob.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("가상피팅 작업과 소유 회원을 함께 조회한다")
    void findFittingJobWithMember() {
        FittingJob savedJob = fittingJobRepository.saveAndFlush(
                FittingJob.create(member)
        );
        entityManager.clear();

        FittingJob fittingJob = fittingJobRepository
                .findByIdWithMember(savedJob.getId())
                .orElseThrow();

        assertThat(fittingJob.getMember().getEmail())
                .isEqualTo("fitting-job-repository@lookddak.com");
        assertThat(Persistence.getPersistenceUtil().isLoaded(
                fittingJob,
                "member"
        )).isTrue();
    }

    @Test
    @DisplayName("가상피팅 작업 상품을 Product와 함께 생성 순서로 조회한다")
    void findFittingJobProductsWithProduct() {
        FittingJob fittingJob = fittingJobRepository.saveAndFlush(
                FittingJob.create(member)
        );
        Product top = productRepository.save(createProduct(
                "상의",
                ProductItemType.TOP
        ));
        Product bottom = productRepository.save(createProduct(
                "하의",
                ProductItemType.BOTTOM
        ));
        fittingJobProductRepository.saveAllAndFlush(List.of(
                FittingJobProduct.create(fittingJob, top),
                FittingJobProduct.create(fittingJob, bottom)
        ));
        entityManager.clear();

        List<FittingJobProduct> jobProducts = fittingJobProductRepository
                .findAllByFittingJobIdWithProduct(fittingJob.getId());

        assertThat(jobProducts)
                .extracting(jobProduct -> jobProduct.getProduct().getId())
                .containsExactly(top.getId(), bottom.getId());
        assertThat(Persistence.getPersistenceUtil().isLoaded(
                jobProducts.get(0),
                "product"
        )).isTrue();
    }

    @Test
    @DisplayName("한 작업에 같은 상품을 중복 저장할 수 없다")
    void rejectDuplicatedFittingJobProduct() {
        FittingJob fittingJob = fittingJobRepository.saveAndFlush(
                FittingJob.create(member)
        );
        Product product = productRepository.saveAndFlush(createProduct(
                "상의",
                ProductItemType.TOP
        ));
        fittingJobProductRepository.saveAndFlush(
                FittingJobProduct.create(fittingJob, product)
        );

        assertThatThrownBy(() -> fittingJobProductRepository.saveAndFlush(
                FittingJobProduct.create(fittingJob, product)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("완료된 작업의 임시 결과를 작업 ID로 조회한다")
    void findTempResultByFittingJobId() {
        FittingJob fittingJob = fittingJobRepository.saveAndFlush(
                FittingJob.create(member)
        );
        fittingJob.completeGeneration();
        FittingTempResult savedResult = fittingTempResultRepository
                .saveAndFlush(createTempResult(fittingJob));
        entityManager.clear();

        FittingTempResult result = fittingTempResultRepository
                .findByFittingJobId(fittingJob.getId())
                .orElseThrow();

        assertThat(result.getId()).isEqualTo(savedResult.getId());
        assertThat(result.getResultImageKey())
                .isEqualTo("fittings/result.jpg");
        assertThat(result.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("한 작업에는 임시 결과를 하나만 저장할 수 있다")
    void rejectDuplicatedTempResult() {
        FittingJob fittingJob = fittingJobRepository.saveAndFlush(
                FittingJob.create(member)
        );
        fittingTempResultRepository.saveAndFlush(createTempResult(fittingJob));

        assertThatThrownBy(() -> fittingTempResultRepository.saveAndFlush(
                createTempResult(fittingJob)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    private Product createProduct(
            String name,
            ProductItemType itemType
    ) {
        return Product.create(
                "code-" + name,
                name,
                "https://image.lookddak.com/products/test.jpg",
                49_000,
                "차콜",
                itemType,
                "https://shop.lookddak.com/products/test"
        );
    }

    private FittingTempResult createTempResult(FittingJob fittingJob) {
        return FittingTempResult.create(
                fittingJob,
                "fittings/result.jpg",
                "가을 출근 니트 룩",
                "선택한 상하의 조합이 자연스럽게 어우러져 있어요."
        );
    }
}
