package com.ktb.lookddak.domain.fitting.entity;

import com.ktb.lookddak.domain.member.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class FittingResultTest {

    @Test
    @DisplayName("가상피팅 임시 결과와 최종 코디명으로 저장 결과를 생성한다")
    void createFittingResult() {
        Member member = Member.create("result@lookddak.com", "encoded-password");
        ReflectionTestUtils.setField(member, "id", 1L);
        FittingJob fittingJob = FittingJob.create(member);
        ReflectionTestUtils.setField(fittingJob, "id", 10L);

        FittingResult fittingResult = FittingResult.create(
                fittingJob,
                "fitting/results/10.png",
                "회사 데일리 니트",
                "편안한 출근용 코디입니다."
        );

        assertThat(fittingResult.getFittingJob()).isSameAs(fittingJob);
        assertThat(fittingResult.getResultImageKey())
                .isEqualTo("fitting/results/10.png");
        assertThat(fittingResult.getOutfitName()).isEqualTo("회사 데일리 니트");
        assertThat(fittingResult.getAiComment())
                .isEqualTo("편안한 출근용 코디입니다.");
        assertThat(fittingResult.isOwnedBy(1L)).isTrue();
        assertThat(fittingResult.isOwnedBy(2L)).isFalse();
    }
}
