package com.ktb.lookddak.domain.fitting.entity;

import com.ktb.lookddak.domain.member.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FittingJobTest {

    @Test
    @DisplayName("가상피팅 작업은 GENERATING 상태로 생성된다")
    void createFittingJob() {
        Member member = createMember(1L);

        FittingJob fittingJob = FittingJob.create(member);

        assertThat(fittingJob.getMember()).isSameAs(member);
        assertThat(fittingJob.getStatus())
                .isEqualTo(FittingJobStatus.GENERATING);
        assertThat(fittingJob.isOwnedBy(1L)).isTrue();
        assertThat(fittingJob.isOwnedBy(2L)).isFalse();
    }

    @Test
    @DisplayName("생성 중인 가상피팅 작업을 완료 상태로 변경한다")
    void completeGeneration() {
        FittingJob fittingJob = FittingJob.create(createMember(1L));

        fittingJob.completeGeneration();

        assertThat(fittingJob.getStatus())
                .isEqualTo(FittingJobStatus.COMPLETED);
    }

    @Test
    @DisplayName("생성 중인 가상피팅 작업을 실패 상태로 변경한다")
    void failGeneration() {
        FittingJob fittingJob = FittingJob.create(createMember(1L));

        fittingJob.failGeneration();

        assertThat(fittingJob.getStatus())
                .isEqualTo(FittingJobStatus.FAILED);
    }

    @Test
    @DisplayName("종료된 가상피팅 작업의 상태는 다시 변경할 수 없다")
    void rejectStatusChangeAfterCompletion() {
        FittingJob fittingJob = FittingJob.create(createMember(1L));
        fittingJob.completeGeneration();

        assertThatThrownBy(fittingJob::failGeneration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("생성 중인 가상피팅 작업만 상태를 변경할 수 있습니다.");
    }

    private Member createMember(Long memberId) {
        Member member = Member.create(
                "member" + memberId + "@lookddak.com",
                "encoded-password"
        );
        ReflectionTestUtils.setField(member, "id", memberId);
        return member;
    }
}
