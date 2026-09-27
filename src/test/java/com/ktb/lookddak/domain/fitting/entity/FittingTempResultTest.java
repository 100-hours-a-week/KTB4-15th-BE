package com.ktb.lookddak.domain.fitting.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class FittingTempResultTest {

    @Test
    @DisplayName("가상피팅 작업의 임시 결과를 생성한다")
    void createFittingTempResult() {
        FittingJob fittingJob = mock(FittingJob.class);

        FittingTempResult result = FittingTempResult.create(
                fittingJob,
                "fittings/result.jpg",
                "가을 출근 니트 룩",
                "선택한 상하의 조합이 자연스럽게 어우러져 있어요."
        );

        assertThat(result.getFittingJob()).isSameAs(fittingJob);
        assertThat(result.getResultImageKey())
                .isEqualTo("fittings/result.jpg");
        assertThat(result.getOutfitName()).isEqualTo("가을 출근 니트 룩");
        assertThat(result.getAiComment())
                .isEqualTo("선택한 상하의 조합이 자연스럽게 어우러져 있어요.");
    }
}
