package com.ktb.lookddak.domain.fitting.mq;

import com.ktb.lookddak.domain.fitting.mq.message.FittingResultMessage;
import com.ktb.lookddak.domain.fitting.mq.message.FittingResultStatus;
import com.ktb.lookddak.domain.fitting.service.FittingGenerationResultService;
import com.ktb.lookddak.domain.notification.event.FittingNotificationCreatedEvent;
import com.ktb.lookddak.global.client.ai.fitting.dto.AiFittingResultData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FittingResultMessageHandler {

    private final FittingGenerationResultService resultService;

    public Optional<FittingNotificationCreatedEvent> handle(
            FittingResultMessage message
    ) {
        validateCommonFields(message);

        if (message.getStatus() == FittingResultStatus.COMPLETED) {
            validateCompletedMessage(message);
            return resultService.complete(message.getFittingJobId(),
                    new AiFittingResultData(
                            message.getResultImageKey(),
                            message.getLlmTitle(),
                            message.getLlmComment()
                    ));
        }

        validateFailedMessage(message);
        return resultService.fail(
                message.getFittingJobId(),
                message.getErrorCode(),
                message.getErrorMessage()
        );
    }

    private void validateCommonFields(FittingResultMessage message) {
        if (message == null || message.getFittingJobId() == null
                || message.getFittingJobId() <= 0 || message.getStatus() == null) {
            throw new IllegalArgumentException("가상피팅 결과 메시지 형식이 올바르지 않습니다.");
        }
    }

    private void validateCompletedMessage(FittingResultMessage message) {
        if (!StringUtils.hasText(message.getResultImageKey())
                || !StringUtils.hasText(message.getLlmTitle())
                || !StringUtils.hasText(message.getLlmComment())) {
            throw new IllegalArgumentException("성공 가상피팅 결과에 필수 값이 없습니다.");
        }
    }

    private void validateFailedMessage(FittingResultMessage message) {
        if (!StringUtils.hasText(message.getErrorCode())
                || !StringUtils.hasText(message.getErrorMessage())) {
            throw new IllegalArgumentException("실패 가상피팅 결과에 오류 정보가 없습니다.");
        }
    }
}
