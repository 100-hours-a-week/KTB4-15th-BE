package com.ktb.lookddak.domain.fitting.service;

import com.ktb.lookddak.domain.fitting.command.FittingGenerationCommand;
import com.ktb.lookddak.domain.fitting.entity.FittingJob;
import com.ktb.lookddak.domain.fitting.entity.FittingJobProduct;
import com.ktb.lookddak.domain.fitting.repository.FittingJobProductRepository;
import com.ktb.lookddak.domain.fitting.repository.FittingJobRepository;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.member.entity.MemberProfile;
import com.ktb.lookddak.domain.member.repository.MemberProfileRepository;
import com.ktb.lookddak.domain.product.entity.Product;
import com.ktb.lookddak.domain.product.entity.ProductItemType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FittingGenerationQueryServiceTest {

    @Mock
    private FittingJobRepository fittingJobRepository;
    @Mock
    private FittingJobProductRepository fittingJobProductRepository;
    @Mock
    private MemberProfileRepository memberProfileRepository;

    private FittingGenerationQueryService service;

    @BeforeEach
    void setUp() {
        service = new FittingGenerationQueryService(
                fittingJobRepository,
                fittingJobProductRepository,
                memberProfileRepository
        );
    }

    @Test
    @DisplayName("작업의 전신사진 Key와 상품 코드를 AI 요청 정보로 조회한다")
    void createCommand() {
        Member member = member(1L);
        FittingJob fittingJob = fittingJob(10L, member);
        MemberProfile profile = profile(member);
        FittingJobProduct top = jobProduct(
                fittingJob,
                product("10001", ProductItemType.TOP)
        );
        FittingJobProduct bottom = jobProduct(
                fittingJob,
                product("20002", ProductItemType.BOTTOM)
        );

        given(fittingJobRepository.findByIdWithMember(10L))
                .willReturn(Optional.of(fittingJob));
        given(memberProfileRepository.findByMemberId(1L))
                .willReturn(Optional.of(profile));
        given(fittingJobProductRepository
                .findAllByFittingJobIdWithProduct(10L))
                .willReturn(List.of(top, bottom));

        FittingGenerationCommand command = service.createCommand(10L);

        assertThat(command.getFittingJobId()).isEqualTo(10L);
        assertThat(command.getFullBodyImageKey())
                .isEqualTo("users/1/body-images/validated.png");
        assertThat(command.getProductCodes())
                .containsExactly("10001", "20002");
    }

    @Test
    @DisplayName("회원 프로필이 없으면 AI 요청 정보를 생성하지 않는다")
    void rejectMissingProfile() {
        Member member = member(1L);
        FittingJob fittingJob = fittingJob(10L, member);
        given(fittingJobRepository.findByIdWithMember(10L))
                .willReturn(Optional.of(fittingJob));
        given(memberProfileRepository.findByMemberId(1L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.createCommand(10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("AI 가상피팅에 사용할 회원 프로필이 없습니다.");
    }

    private Member member(Long id) {
        Member member = Member.create(
                "member@test.com",
                "encoded-password"
        );
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private FittingJob fittingJob(Long id, Member member) {
        FittingJob fittingJob = FittingJob.create(member);
        ReflectionTestUtils.setField(fittingJob, "id", id);
        return fittingJob;
    }

    private MemberProfile profile(Member member) {
        return MemberProfile.create(
                member,
                "Tester",
                29,
                new BigDecimal("175.0"),
                new BigDecimal("70.0"),
                "users/1/body-images/validated.png"
        );
    }

    private Product product(
            String productCode,
            ProductItemType itemType
    ) {
        return Product.create(
                productCode,
                "Product " + productCode,
                "https://example.com/" + productCode + ".png",
                50000,
                "BLACK",
                itemType,
                "https://example.com/products/" + productCode
        );
    }

    private FittingJobProduct jobProduct(
            FittingJob fittingJob,
            Product product
    ) {
        return FittingJobProduct.create(fittingJob, product);
    }
}
