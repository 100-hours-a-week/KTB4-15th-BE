package com.ktb.lookddak.domain.fitting.dto;

import com.ktb.lookddak.domain.fitting.entity.FittingCandidate;
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

class FittingCandidateListResponseTest {

    @Test
    @DisplayName("피팅 후보와 상품 정보를 목록 항목 응답으로 변환한다")
    void createItemFromFittingCandidate() {
        FittingCandidate candidate = createCandidate(30L, 101L);

        FittingCandidateListItemResponse item =
                FittingCandidateListItemResponse.from(candidate);

        assertThat(item.getFittingCandidateId()).isEqualTo(30L);
        assertThat(item.getProductId()).isEqualTo(101L);
        assertThat(item.getProductName()).isEqualTo("에센셜 램스울 크루넥");
        assertThat(item.getProductImageUrl())
                .isEqualTo("https://image.lookddak.com/products/101.jpg");
        assertThat(item.getCurrentPrice()).isEqualTo(49_000);
        assertThat(item.getColor()).isEqualTo("차콜");
        assertThat(item.getItemType()).isEqualTo(ProductItemType.TOP);
    }

    @Test
    @DisplayName("다음 페이지가 있으면 마지막 피팅 후보 ID를 Cursor로 반환한다")
    void createResponseWithNextPage() {
        FittingCandidateListItemResponse item =
                FittingCandidateListItemResponse.from(
                        createCandidate(29L, 102L)
                );

        FittingCandidateListResponse response =
                new FittingCandidateListResponse(
                        List.of(item),
                        29L,
                        true
                );

        assertThat(response.getItems()).containsExactly(item);
        assertThat(response.getNextCursor()).isEqualTo(29L);
        assertThat(response.isHasNext()).isTrue();
    }

    @Test
    @DisplayName("조회 결과가 없으면 빈 목록과 null Cursor를 반환한다")
    void createEmptyResponse() {
        FittingCandidateListResponse response =
                new FittingCandidateListResponse(
                        List.of(),
                        null,
                        false
                );

        assertThat(response.getItems()).isEmpty();
        assertThat(response.getNextCursor()).isNull();
        assertThat(response.isHasNext()).isFalse();
    }

    @Test
    @DisplayName("응답 목록은 생성 후 외부에서 변경할 수 없다")
    void copyItems() {
        FittingCandidateListItemResponse item =
                FittingCandidateListItemResponse.from(
                        createCandidate(28L, 103L)
                );
        List<FittingCandidateListItemResponse> items = new ArrayList<>();
        items.add(item);

        FittingCandidateListResponse response =
                new FittingCandidateListResponse(items, null, false);
        items.clear();

        assertThat(response.getItems()).containsExactly(item);
    }

    @Test
    @DisplayName("피팅 후보 목록을 API 명세의 필드 순서로 직렬화한다")
    void serializeResponseInSpecifiedOrder() throws Exception {
        FittingCandidateListItemResponse item =
                FittingCandidateListItemResponse.from(
                        createCandidate(30L, 101L)
                );
        FittingCandidateListResponse response =
                new FittingCandidateListResponse(
                        List.of(item),
                        30L,
                        true
                );
        ObjectMapper objectMapper = JsonMapper.builder()
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .build();

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).containsSubsequence(
                "\"items\"",
                "\"nextCursor\"",
                "\"hasNext\""
        );
        assertThat(json).containsSubsequence(
                "\"fittingCandidateId\"",
                "\"productId\"",
                "\"productName\"",
                "\"productImageUrl\"",
                "\"currentPrice\"",
                "\"color\"",
                "\"itemType\""
        );
        assertThat(json).doesNotContain("saleStatus", "purchaseUrl");
    }

    private FittingCandidate createCandidate(
            Long candidateId,
            Long productId
    ) {
        Member member = Member.create(
                "member@lookddak.com",
                "encoded-password"
        );
        Product product = Product.create(
                "product-" + productId,
                "에센셜 램스울 크루넥",
                "https://image.lookddak.com/products/101.jpg",
                49_000,
                "차콜",
                ProductItemType.TOP,
                "https://shop.lookddak.com/products/101"
        );
        ReflectionTestUtils.setField(product, "id", productId);

        FittingCandidate candidate = FittingCandidate.create(member, product);
        ReflectionTestUtils.setField(candidate, "id", candidateId);
        return candidate;
    }
}
