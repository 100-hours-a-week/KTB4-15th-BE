package com.ktb.lookddak.domain.fitting.service;

import com.ktb.lookddak.domain.fitting.entity.FittingJob;
import com.ktb.lookddak.domain.fitting.entity.FittingJobStatus;
import com.ktb.lookddak.domain.fitting.entity.FittingTempResult;
import com.ktb.lookddak.domain.fitting.repository.FittingJobRepository;
import com.ktb.lookddak.domain.fitting.repository.FittingTempResultRepository;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.global.client.ai.fitting.dto.AiFittingResultData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FittingGenerationResultServiceTest {

    @Mock
    private FittingJobRepository fittingJobRepository;

    @Mock
    private FittingTempResultRepository fittingTempResultRepository;

    @InjectMocks
    private FittingGenerationResultService resultService;

    @Test
    @DisplayName("AI 결과를 저장하고 가상피팅 작업을 완료한다")
    void completeGeneration() {
        FittingJob fittingJob = FittingJob.create(Member.create(
                "fitting-result@lookddak.com",
                "encoded-password"
        ));
        given(fittingJobRepository.findByIdForGenerationUpdate(100L))
                .willReturn(Optional.of(fittingJob));
        AiFittingResultData result = new AiFittingResultData(
                "fittings/result.png",
                "차분한 데일리 룩",
                "깔끔한 색감이 어우러지는 코디입니다."
        );

        resultService.complete(100L, result);

        ArgumentCaptor<FittingTempResult> captor =
                ArgumentCaptor.forClass(FittingTempResult.class);
        verify(fittingTempResultRepository).save(captor.capture());
        assertThat(captor.getValue().getResultImageKey())
                .isEqualTo("fittings/result.png");
        assertThat(captor.getValue().getOutfitName())
                .isEqualTo("차분한 데일리 룩");
        assertThat(captor.getValue().getAiComment())
                .isEqualTo("깔끔한 색감이 어우러지는 코디입니다.");
        assertThat(fittingJob.getStatus())
                .isEqualTo(FittingJobStatus.COMPLETED);
    }

    @Test
    @DisplayName("AI 생성 실패 시 가상피팅 작업을 실패 처리한다")
    void failGeneration() {
        FittingJob fittingJob = FittingJob.create(Member.create(
                "fitting-failed@lookddak.com",
                "encoded-password"
        ));
        given(fittingJobRepository.findByIdForGenerationUpdate(100L))
                .willReturn(Optional.of(fittingJob));

        resultService.fail(100L);

        assertThat(fittingJob.getStatus()).isEqualTo(FittingJobStatus.FAILED);
        verify(fittingTempResultRepository, never()).save(
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    @DisplayName("이미 처리된 작업의 중복 AI 결과는 다시 저장하지 않는다")
    void ignoreDuplicatedResult() {
        FittingJob fittingJob = FittingJob.create(Member.create(
                "fitting-duplicated@lookddak.com",
                "encoded-password"
        ));
        fittingJob.completeGeneration();
        given(fittingJobRepository.findByIdForGenerationUpdate(100L))
                .willReturn(Optional.of(fittingJob));

        resultService.complete(100L, new AiFittingResultData(
                "fittings/result.png",
                "코디명",
                "코디 설명"
        ));

        verify(fittingTempResultRepository, never()).save(
                org.mockito.ArgumentMatchers.any()
        );
    }
}
