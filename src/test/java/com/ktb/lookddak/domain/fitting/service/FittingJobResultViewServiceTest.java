package com.ktb.lookddak.domain.fitting.service;

import com.ktb.lookddak.domain.fitting.entity.FittingJob;
import com.ktb.lookddak.domain.fitting.entity.FittingTempResult;
import com.ktb.lookddak.domain.fitting.repository.FittingJobRepository;
import com.ktb.lookddak.domain.fitting.repository.FittingTempResultRepository;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.global.exception.BusinessException;
import com.ktb.lookddak.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FittingJobResultViewServiceTest {

    @Mock
    private FittingJobRepository fittingJobRepository;

    @Mock
    private FittingTempResultRepository fittingTempResultRepository;

    private FittingJobResultViewService service;

    @BeforeEach
    void setUp() {
        service = new FittingJobResultViewService(
                fittingJobRepository,
                fittingTempResultRepository
        );
    }

    @Test
    @DisplayName("완료된 임시 결과를 확인 처리한다")
    void markCompletedResultViewed() {
        FittingJob fittingJob = completedJob(100L, 1L);
        given(fittingJobRepository.findByIdWithMemberForUpdate(100L))
                .willReturn(Optional.of(fittingJob));
        given(fittingTempResultRepository.findByFittingJobId(100L))
                .willReturn(Optional.of(tempResult(fittingJob)));

        service.markResultViewed(1L, 100L);

        assertThat(fittingJob.isResultViewed()).isTrue();
        assertThat(fittingJob.getResultViewedAt()).isNotNull();
    }

    @Test
    @DisplayName("이미 확인한 결과는 확인 시각을 변경하지 않는다")
    void keepOriginalViewedAt() {
        FittingJob fittingJob = completedJob(100L, 1L);
        LocalDateTime firstViewedAt = LocalDateTime.now().minusMinutes(1);
        ReflectionTestUtils.setField(
                fittingJob,
                "resultViewedAt",
                firstViewedAt
        );
        given(fittingJobRepository.findByIdWithMemberForUpdate(100L))
                .willReturn(Optional.of(fittingJob));
        given(fittingTempResultRepository.findByFittingJobId(100L))
                .willReturn(Optional.of(tempResult(fittingJob)));

        service.markResultViewed(1L, 100L);

        assertThat(fittingJob.getResultViewedAt()).isEqualTo(firstViewedAt);
    }

    @Test
    @DisplayName("생성 중인 작업은 확인 처리할 수 없다")
    void rejectGeneratingJob() {
        FittingJob fittingJob = job(100L, 1L);
        given(fittingJobRepository.findByIdWithMemberForUpdate(100L))
                .willReturn(Optional.of(fittingJob));

        assertThatThrownBy(() -> service.markResultViewed(1L, 100L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FITTING_JOB_NOT_COMPLETED);
    }

    @Test
    @DisplayName("만료된 임시 결과는 확인 처리할 수 없다")
    void rejectExpiredTempResult() {
        FittingJob fittingJob = completedJob(100L, 1L);
        FittingTempResult tempResult = tempResult(fittingJob);
        ReflectionTestUtils.setField(
                tempResult,
                "expiresAt",
                LocalDateTime.now().minusSeconds(1)
        );
        given(fittingJobRepository.findByIdWithMemberForUpdate(100L))
                .willReturn(Optional.of(fittingJob));
        given(fittingTempResultRepository.findByFittingJobId(100L))
                .willReturn(Optional.of(tempResult));

        assertThatThrownBy(() -> service.markResultViewed(1L, 100L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FITTING_TEMP_RESULT_NOT_FOUND);
    }

    private FittingJob completedJob(Long fittingJobId, Long memberId) {
        FittingJob fittingJob = job(fittingJobId, memberId);
        fittingJob.completeGeneration();
        return fittingJob;
    }

    private FittingJob job(Long fittingJobId, Long memberId) {
        Member member = Member.create(
                "member" + memberId + "@lookddak.com",
                "encoded-password"
        );
        ReflectionTestUtils.setField(member, "id", memberId);
        FittingJob fittingJob = FittingJob.create(member);
        ReflectionTestUtils.setField(fittingJob, "id", fittingJobId);
        return fittingJob;
    }

    private FittingTempResult tempResult(FittingJob fittingJob) {
        return FittingTempResult.create(
                fittingJob,
                "fittings/result.png",
                "출근 룩",
                "코디 설명"
        );
    }
}
