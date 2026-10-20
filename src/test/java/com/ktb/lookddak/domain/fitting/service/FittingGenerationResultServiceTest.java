package com.ktb.lookddak.domain.fitting.service;

import com.ktb.lookddak.domain.fitting.entity.FittingJob;
import com.ktb.lookddak.domain.fitting.entity.FittingJobStatus;
import com.ktb.lookddak.domain.fitting.entity.FittingTempResult;
import com.ktb.lookddak.domain.fitting.repository.FittingJobRepository;
import com.ktb.lookddak.domain.fitting.repository.FittingTempResultRepository;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.member.repository.MemberRepository;
import com.ktb.lookddak.domain.notification.event.FittingNotificationCreatedEvent;
import com.ktb.lookddak.domain.notification.repository.NotificationRepository;
import com.ktb.lookddak.global.client.ai.fitting.dto.AiFittingResultData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FittingGenerationResultServiceTest {

    @Mock
    private FittingJobRepository fittingJobRepository;

    @Mock
    private FittingTempResultRepository fittingTempResultRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private MemberRepository memberRepository;

    private FittingGenerationResultService resultService;

    private final Clock clock = Clock.fixed(
            Instant.parse("2026-10-21T00:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @BeforeEach
    void setUp() {
        resultService = new FittingGenerationResultService(
                fittingJobRepository,
                fittingTempResultRepository,
                notificationRepository,
                memberRepository,
                clock
        );
    }

    @Test
    @DisplayName("AI 결과를 저장하고 가상피팅 작업을 완료한다")
    void completeGeneration() {
        FittingJob fittingJob = FittingJob.create(Member.create(
                "fitting-result@lookddak.com",
                "encoded-password"
        ));
        given(fittingJobRepository.findByIdWithMemberForResultSave(100L))
                .willReturn(Optional.of(fittingJob));
        given(notificationRepository.save(any()))
                .willAnswer(invocation -> invocation.getArgument(0));
        AiFittingResultData result = new AiFittingResultData(
                "fittings/result.png",
                "차분한 데일리 룩",
                "깔끔한 색감이 어우러지는 코디입니다."
        );

        var notificationEvent = resultService.complete(100L, result);

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
        then(notificationRepository).should().save(any());
        assertThat(notificationEvent).isPresent();
    }

    @Test
    @DisplayName("AI 생성 실패 시 가상피팅 작업을 실패 처리한다")
    void failGeneration() {
        LocalDate quotaDate = LocalDate.of(2026, 10, 21);
        Member member = Member.create(
                "fitting-fail@lookddak.com",
                "encoded-password"
        );
        ReflectionTestUtils.setField(member, "id", 1L);
        member.consumeFittingRequestQuota(quotaDate);
        FittingJob fittingJob = FittingJob.create(member, quotaDate);
        given(fittingJobRepository.findByIdWithMemberForResultSave(100L))
                .willReturn(Optional.of(fittingJob));
        given(memberRepository.findActiveByIdForUpdate(1L)).willReturn(Optional.of(member));
        given(notificationRepository.save(any()))
                .willAnswer(invocation -> invocation.getArgument(0));

        var notificationEvent = resultService.fail(100L);

        assertThat(fittingJob.getStatus()).isEqualTo(FittingJobStatus.FAILED);
        assertThat(member.getFittingRequestCount()).isZero();
        assertThat(fittingJob.getQuotaRefundedAt()).isNotNull();
        then(notificationRepository).should().save(any());
        assertThat(notificationEvent).isPresent();
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
        given(fittingJobRepository.findByIdWithMemberForResultSave(100L))
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

    @Test
    @DisplayName("중복된 AI 실패 결과는 이용권을 한 번만 환불한다")
    void refundQuotaOnlyOnceForDuplicatedFailure() {
        LocalDate quotaDate = LocalDate.of(2026, 10, 21);
        Member member = Member.create("fitting-duplicate-fail@lookddak.com", "encoded-password");
        ReflectionTestUtils.setField(member, "id", 1L);
        member.consumeFittingRequestQuota(quotaDate);
        FittingJob fittingJob = FittingJob.create(member, quotaDate);
        given(fittingJobRepository.findByIdWithMemberForResultSave(100L))
                .willReturn(Optional.of(fittingJob));
        given(memberRepository.findActiveByIdForUpdate(1L)).willReturn(Optional.of(member));
        given(notificationRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        resultService.fail(100L, "AI_FAILED", "AI 생성에 실패했습니다.");
        resultService.fail(100L, "AI_FAILED", "AI 생성에 실패했습니다.");

        assertThat(member.getFittingRequestCount()).isZero();
        verify(memberRepository).findActiveByIdForUpdate(1L);
        verify(notificationRepository).save(any());
    }

    @Test
    @DisplayName("전날 차감된 작업이 늦게 실패해도 오늘의 이용권은 환불하지 않는다")
    void doNotRefundCurrentDayQuotaForPreviousDayJob() {
        LocalDate yesterday = LocalDate.of(2026, 10, 20);
        LocalDate today = yesterday.plusDays(1);
        Member member = Member.create("fitting-next-day-fail@lookddak.com", "encoded-password");
        ReflectionTestUtils.setField(member, "id", 1L);
        member.consumeFittingRequestQuota(yesterday);
        member.consumeFittingRequestQuota(today);
        FittingJob fittingJob = FittingJob.create(member, yesterday);
        given(fittingJobRepository.findByIdWithMemberForResultSave(100L))
                .willReturn(Optional.of(fittingJob));
        given(memberRepository.findActiveByIdForUpdate(1L)).willReturn(Optional.of(member));
        given(notificationRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        resultService.fail(100L, "AI_FAILED", "AI 생성에 실패했습니다.");

        assertThat(member.getFittingRequestDate()).isEqualTo(today);
        assertThat(member.getFittingRequestCount()).isEqualTo(1);
        assertThat(fittingJob.getQuotaRefundedAt()).isNotNull();
    }
}
