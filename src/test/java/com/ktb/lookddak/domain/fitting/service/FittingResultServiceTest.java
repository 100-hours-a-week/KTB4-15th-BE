package com.ktb.lookddak.domain.fitting.service;

import com.ktb.lookddak.domain.fitting.dto.FittingResultCreateRequest;
import com.ktb.lookddak.domain.fitting.dto.FittingResultCreateResponse;
import com.ktb.lookddak.domain.fitting.dto.FittingResultListResponse;
import com.ktb.lookddak.domain.fitting.entity.FittingJob;
import com.ktb.lookddak.domain.fitting.entity.FittingResult;
import com.ktb.lookddak.domain.fitting.entity.FittingTempResult;
import com.ktb.lookddak.domain.fitting.repository.FittingJobRepository;
import com.ktb.lookddak.domain.fitting.repository.FittingResultRepository;
import com.ktb.lookddak.domain.fitting.repository.FittingTempResultRepository;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.global.exception.BusinessException;
import com.ktb.lookddak.global.exception.ErrorCode;
import com.ktb.lookddak.global.storage.s3.S3PresignedUrlProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
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

    @Mock
    private S3PresignedUrlProvider presignedUrlProvider;

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

    @Test
    @DisplayName("가상피팅 결과 목록은 최신순으로 조회하고 다음 커서를 반환한다")
    void getFittingResults() {
        FittingResultService service = createService();
        FittingResult first = fittingResult(1L, 30L, "fittings/30.png");
        FittingResult second = fittingResult(1L, 29L, "fittings/29.png");
        FittingResult extra = fittingResult(1L, 28L, "fittings/28.png");
        given(fittingResultRepository.findFirstPage(any(), any()))
                .willReturn(List.of(first, second, extra));
        given(fittingResultRepository.countActiveByMemberId(1L))
                .willReturn(3L);
        given(presignedUrlProvider.createGetUrl("fittings/30.png"))
                .willReturn("https://image.example.com/fittings/30.png");
        given(presignedUrlProvider.createGetUrl("fittings/29.png"))
                .willReturn("https://image.example.com/fittings/29.png");

        FittingResultListResponse response = service.getFittingResults(
                1L,
                null,
                2
        );

        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getItems().get(0).getFittingResultId())
                .isEqualTo(30L);
        assertThat(response.getNextCursor()).isEqualTo(29L);
        assertThat(response.isHasNext()).isTrue();
        assertThat(response.getTotalCount()).isEqualTo(3L);
        verify(presignedUrlProvider).createGetUrl("fittings/30.png");
        verify(presignedUrlProvider).createGetUrl("fittings/29.png");
    }

    @Test
    @DisplayName("저장된 가상피팅 결과가 없으면 빈 목록을 반환한다")
    void getEmptyFittingResults() {
        FittingResultService service = createService();
        given(fittingResultRepository.findFirstPage(any(), any()))
                .willReturn(List.of());
        given(fittingResultRepository.countActiveByMemberId(1L))
                .willReturn(0L);

        FittingResultListResponse response = service.getFittingResults(
                1L,
                null,
                null
        );

        assertThat(response.getItems()).isEmpty();
        assertThat(response.getTotalCount()).isZero();
        assertThat(response.getNextCursor()).isNull();
        assertThat(response.isHasNext()).isFalse();
    }

    @Test
    @DisplayName("잘못된 목록 조회 조건은 거부한다")
    void rejectInvalidPagination() {
        FittingResultService service = createService();

        assertThatThrownBy(() -> service.getFittingResults(1L, 0L, 20))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_PAGINATION_PARAMETER)
                );
    }

    private FittingResultService createService() {
        return new FittingResultService(
                fittingJobRepository,
                fittingTempResultRepository,
                fittingResultRepository,
                presignedUrlProvider
        );
    }

    private FittingResult fittingResult(
            Long memberId,
            Long fittingResultId,
            String imageKey
    ) {
        FittingJob fittingJob = completedFittingJob(memberId, fittingResultId);
        FittingResult fittingResult = FittingResult.create(
                fittingJob,
                imageKey,
                "출근룩",
                "AI 코디 설명"
        );
        ReflectionTestUtils.setField(fittingResult, "id", fittingResultId);
        return fittingResult;
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
