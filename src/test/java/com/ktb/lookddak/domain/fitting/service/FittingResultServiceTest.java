package com.ktb.lookddak.domain.fitting.service;

import com.ktb.lookddak.domain.fitting.dto.FittingResultCreateRequest;
import com.ktb.lookddak.domain.fitting.dto.FittingResultCreateResponse;
import com.ktb.lookddak.domain.fitting.entity.FittingJob;
import com.ktb.lookddak.domain.fitting.entity.FittingResult;
import com.ktb.lookddak.domain.fitting.entity.FittingTempResult;
import com.ktb.lookddak.domain.fitting.repository.FittingJobRepository;
import com.ktb.lookddak.domain.fitting.repository.FittingResultRepository;
import com.ktb.lookddak.domain.fitting.repository.FittingTempResultRepository;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.global.exception.BusinessException;
import com.ktb.lookddak.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FittingResultServiceTest {

    @Mock
    private FittingJobRepository fittingJobRepository;

    @Mock
    private FittingTempResultRepository fittingTempResultRepository;

    @Mock
    private FittingResultRepository fittingResultRepository;

    @Test
    @DisplayName("완료된 가상피팅 임시 결과를 저장 결과로 생성한다")
    void createFittingResult() {
        FittingResultService service = createService();
        FittingJob fittingJob = completedFittingJob(1L, 10L);
        FittingTempResult tempResult = FittingTempResult.create(
                fittingJob,
                "fittings/result-10.png",
                "AI 코디명",
                "AI 코디 설명"
        );
        given(fittingJobRepository.findByIdWithMemberForResultSave(10L))
                .willReturn(Optional.of(fittingJob));
        given(fittingTempResultRepository.findByFittingJobId(10L))
                .willReturn(Optional.of(tempResult));
        given(fittingResultRepository.existsByFittingJobId(10L)).willReturn(false);
        given(fittingResultRepository.saveAndFlush(any(FittingResult.class)))
                .willAnswer(invocation -> {
                    FittingResult fittingResult = invocation.getArgument(0);
                    ReflectionTestUtils.setField(fittingResult, "id", 100L);
                    return fittingResult;
                });

        FittingResultCreateResponse response = service.createFittingResult(
                1L,
                new FittingResultCreateRequest(10L, "회사 데일리 니트")
        );

        assertThat(response.getFittingResultId()).isEqualTo(100L);
        assertThat(response.getOutfitName()).isEqualTo("회사 데일리 니트");

        ArgumentCaptor<FittingResult> captor =
                ArgumentCaptor.forClass(FittingResult.class);
        verify(fittingResultRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getResultImageKey())
                .isEqualTo("fittings/result-10.png");
        assertThat(captor.getValue().getAiComment()).isEqualTo("AI 코디 설명");
        assertThat(captor.getValue().getOutfitName()).isEqualTo("회사 데일리 니트");
    }

    @Test
    @DisplayName("공백만 있는 코디명은 저장할 수 없다")
    void rejectBlankOutfitName() {
        FittingResultService service = createService();

        assertThatThrownBy(() -> service.createFittingResult(
                1L,
                new FittingResultCreateRequest(10L, "   ")
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_OUTFIT_NAME)
        );

        verify(fittingJobRepository, never()).findByIdWithMemberForResultSave(any());
    }

    @Test
    @DisplayName("20자를 초과한 코디명은 저장할 수 없다")
    void rejectLongOutfitName() {
        FittingResultService service = createService();

        assertThatThrownBy(() -> service.createFittingResult(
                1L,
                new FittingResultCreateRequest(10L, "가".repeat(21))
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_OUTFIT_NAME_LENGTH)
        );
    }

    @Test
    @DisplayName("이미 저장된 가상피팅 결과는 다시 저장할 수 없다")
    void rejectAlreadySavedResult() {
        FittingResultService service = createService();
        FittingJob fittingJob = completedFittingJob(1L, 10L);
        FittingTempResult tempResult = FittingTempResult.create(
                fittingJob, "fittings/result-10.png", "AI 코디명", "AI 코디 설명"
        );
        given(fittingJobRepository.findByIdWithMemberForResultSave(10L))
                .willReturn(Optional.of(fittingJob));
        given(fittingTempResultRepository.findByFittingJobId(10L))
                .willReturn(Optional.of(tempResult));
        given(fittingResultRepository.existsByFittingJobId(10L)).willReturn(true);

        assertThatThrownBy(() -> service.createFittingResult(
                1L,
                new FittingResultCreateRequest(10L, "회사 데일리 니트")
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.FITTING_RESULT_ALREADY_SAVED)
        );

        verify(fittingResultRepository, never()).saveAndFlush(any());
    }

    private FittingResultService createService() {
        return new FittingResultService(
                fittingJobRepository,
                fittingTempResultRepository,
                fittingResultRepository
        );
    }

    private FittingJob completedFittingJob(Long memberId, Long fittingJobId) {
        Member member = Member.create("result-" + memberId + "@lookddak.com", "password");
        ReflectionTestUtils.setField(member, "id", memberId);
        FittingJob fittingJob = FittingJob.create(member);
        ReflectionTestUtils.setField(fittingJob, "id", fittingJobId);
        fittingJob.completeGeneration();
        return fittingJob;
    }
}
