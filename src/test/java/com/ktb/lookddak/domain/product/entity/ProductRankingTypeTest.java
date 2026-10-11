package com.ktb.lookddak.domain.product.entity;

import com.ktb.lookddak.global.exception.BusinessException;
import com.ktb.lookddak.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductRankingTypeTest {

    @Test
    @DisplayName("유효한 랭킹 기준을 enum으로 변환한다")
    void convertValidRankingType() {
        assertThat(ProductRankingType.from("WISH"))
                .isEqualTo(ProductRankingType.WISH);
        assertThat(ProductRankingType.from("CLICK"))
                .isEqualTo(ProductRankingType.CLICK);
    }

    @Test
    @DisplayName("누락되었거나 올바르지 않은 랭킹 기준은 예외 처리한다")
    void rejectInvalidRankingType() {
        assertInvalidType(null);
        assertInvalidType(" ");
        assertInvalidType("POPULAR");
    }

    private void assertInvalidType(String type) {
        assertThatThrownBy(() -> ProductRankingType.from(type))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_RANKING_TYPE);
    }
}
