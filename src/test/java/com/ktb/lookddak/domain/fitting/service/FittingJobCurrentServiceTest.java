package com.ktb.lookddak.domain.fitting.service;

import com.ktb.lookddak.domain.fitting.dto.FittingJobCurrentResponse;
import com.ktb.lookddak.domain.fitting.entity.FittingJob;
import com.ktb.lookddak.domain.fitting.entity.FittingJobProduct;
import com.ktb.lookddak.domain.fitting.entity.FittingJobStatus;
import com.ktb.lookddak.domain.fitting.repository.FittingJobProductRepository;
import com.ktb.lookddak.domain.fitting.repository.FittingJobRepository;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.product.entity.Product;
import com.ktb.lookddak.domain.product.entity.ProductItemType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FittingJobCurrentServiceTest {

    @Mock
    private FittingJobRepository fittingJobRepository;

    @Mock
    private FittingJobProductRepository fittingJobProductRepository;

    private FittingJobCurrentService service;

    @BeforeEach
    void setUp() {
        service = new FittingJobCurrentService(
                fittingJobRepository,
                fittingJobProductRepository
        );
    }

    @Test
    @DisplayName("최근 확인 필요 가상피팅 작업과 사용 상품을 조회한다")
    void getCurrentFittingJob() {
        FittingJob fittingJob = fittingJob(100L);
        Product product = product(10L);
        given(fittingJobRepository.findCurrentJobs(
                eq(1L),
                eq(FittingJobStatus.PENDING),
                eq(FittingJobStatus.COMPLETED),
                eq(FittingJobStatus.FAILED),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).willReturn(List.of(fittingJob));
        given(fittingJobProductRepository
                .findAllByFittingJobIdWithProduct(100L))
                .willReturn(List.of(FittingJobProduct.create(
                        fittingJob,
                        product
                )));

        FittingJobCurrentResponse response = service
                .getCurrentFittingJob(1L);

        assertThat(response.getFittingJobId()).isEqualTo(100L);
        assertThat(response.getStatus()).isEqualTo(
                FittingJobStatus.PENDING
        );
        assertThat(response.isResultViewed()).isFalse();
        assertThat(response.getProducts()).singleElement().satisfies(item -> {
            assertThat(item.getProductId()).isEqualTo(10L);
            assertThat(item.getItemType()).isEqualTo(ProductItemType.TOP);
        });
    }

    @Test
    @DisplayName("확인할 작업이 없으면 null을 반환한다")
    void returnNullWhenNoCurrentFittingJob() {
        given(fittingJobRepository.findCurrentJobs(
                eq(1L),
                eq(FittingJobStatus.PENDING),
                eq(FittingJobStatus.COMPLETED),
                eq(FittingJobStatus.FAILED),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).willReturn(List.of());

        assertThat(service.getCurrentFittingJob(1L)).isNull();
    }

    private FittingJob fittingJob(Long fittingJobId) {
        Member member = Member.create("member@lookddak.com", "password");
        ReflectionTestUtils.setField(member, "id", 1L);
        FittingJob fittingJob = FittingJob.create(member);
        ReflectionTestUtils.setField(fittingJob, "id", fittingJobId);
        return fittingJob;
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
