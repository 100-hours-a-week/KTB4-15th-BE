package com.ktb.lookddak.domain.fitting.mq.outbox;

import com.ktb.lookddak.global.aws.sqs.SqsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private static final int MAX_RETRY_DELAY_SECONDS = 60;

    private final SqsClient sqsClient;
    private final SqsProperties sqsProperties;
    private final OutboxEventPublicationService publicationService;

    public void publish(ClaimedOutboxEvent event) {
        try {
            sendFittingRequest(event);
            publicationService.markPublished(
                    event.outboxEventId(),
                    event.claimedBy()
            );
        } catch (Exception exception) {
            log.warn(
                    "OUTBOX_PUBLISH_FAILED outboxEventId={} fittingJobId={}",
                    event.outboxEventId(),
                    event.fittingJobId(),
                    exception
            );
            publicationService.markPublishFailed(
                    event.outboxEventId(),
                    event.claimedBy(),
                    LocalDateTime.now().plusSeconds(
                            calculateRetryDelaySeconds(event.retryCount())
                    ),
                    exception.getMessage()
            );
        }
    }

    private void sendFittingRequest(ClaimedOutboxEvent event) {
        if (!StringUtils.hasText(sqsProperties.getFittingRequestQueueUrl())) {
            throw new IllegalStateException(
                    "BE_FITTING_REQUEST_QUEUE_URL이 설정되지 않았습니다."
            );
        }

        sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(sqsProperties.getFittingRequestQueueUrl())
                .messageBody(event.payload())
                .build());
    }

    private long calculateRetryDelaySeconds(int retryCount) {
        int exponent = Math.min(retryCount, 5);
        return Math.min(1L << exponent, MAX_RETRY_DELAY_SECONDS);
    }
}
