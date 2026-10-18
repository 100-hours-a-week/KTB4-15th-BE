package com.ktb.lookddak.domain.fitting.service;

import com.ktb.lookddak.domain.fitting.entity.FittingJob;
import com.ktb.lookddak.domain.fitting.entity.FittingJobStatus;
import com.ktb.lookddak.domain.fitting.entity.FittingTempResult;
import com.ktb.lookddak.domain.fitting.repository.FittingJobRepository;
import com.ktb.lookddak.domain.fitting.repository.FittingTempResultRepository;
import com.ktb.lookddak.domain.notification.entity.Notification;
import com.ktb.lookddak.domain.notification.event.FittingNotificationCreatedEvent;
import com.ktb.lookddak.domain.notification.repository.NotificationRepository;
import com.ktb.lookddak.global.client.ai.fitting.dto.AiFittingResultData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FittingGenerationResultService {

    private final FittingJobRepository fittingJobRepository;
    private final FittingTempResultRepository fittingTempResultRepository;
    private final NotificationRepository notificationRepository;

    @Transactional
    public Optional<FittingNotificationCreatedEvent> complete(
            Long fittingJobId,
            AiFittingResultData result
    ) {
        FittingJob fittingJob = getFittingJobForUpdate(fittingJobId);
        if (!isGenerating(fittingJob)) {
            return Optional.empty();
        }

        fittingTempResultRepository.save(FittingTempResult.create(
                fittingJob,
                result.getResultImageKey(),
                result.getLlmTitle(),
                result.getLlmComment()
        ));
        fittingJob.completeGeneration();
        return Optional.of(createNotificationEvent(Notification.fittingSuccess(
                fittingJob.getMember(),
                fittingJob.getId(),
                result.getLlmTitle()
        )));
    }

    @Transactional
    public Optional<FittingNotificationCreatedEvent> fail(Long fittingJobId) {
        return fail(fittingJobId, null, null);
    }

    @Transactional
    public Optional<FittingNotificationCreatedEvent> fail(
            Long fittingJobId,
            String failureCode,
            String failureMessage
    ) {
        FittingJob fittingJob = getFittingJobForUpdate(fittingJobId);
        if (!isGenerating(fittingJob)) {
            return Optional.empty();
        }

        fittingJob.failGeneration(failureCode, failureMessage);
        return Optional.of(createNotificationEvent(Notification.fittingFailure(
                fittingJob.getMember(),
                fittingJob.getId()
        )));
    }

    private FittingJob getFittingJobForUpdate(Long fittingJobId) {
        return fittingJobRepository
                .findByIdWithMemberForResultSave(fittingJobId)
                .orElseThrow(() -> new IllegalStateException(
                        "AI 결과를 연결할 가상피팅 작업이 없습니다."
                ));
    }

    private boolean isGenerating(FittingJob fittingJob) {
        return fittingJob.getStatus() == FittingJobStatus.PENDING;
    }

    private FittingNotificationCreatedEvent createNotificationEvent(
            Notification notification
    ) {
        Notification savedNotification = notificationRepository.save(notification);
        return new FittingNotificationCreatedEvent(
                savedNotification.getId(),
                savedNotification.getMember().getId(),
                savedNotification.getTargetId(),
                savedNotification.getType()
        );
    }
}
