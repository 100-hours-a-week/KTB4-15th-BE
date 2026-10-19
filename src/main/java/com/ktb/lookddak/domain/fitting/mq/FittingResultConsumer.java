package com.ktb.lookddak.domain.fitting.mq;

import com.ktb.lookddak.domain.fitting.mq.message.FittingResultMessage;
import com.ktb.lookddak.domain.notification.event.FittingNotificationCreatedEvent;
import com.ktb.lookddak.global.aws.sqs.SqsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class FittingResultConsumer {

    private final SqsClient sqsClient;
    private final SqsProperties sqsProperties;
    private final FittingResultConsumerProperties properties;
    private final ObjectMapper objectMapper;
    private final FittingResultMessageHandler messageHandler;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(fixedDelayString = "${fitting.result-consumer.poll-interval:1s}")
    public void consume() {
        if (!properties.isEnabled()) {
            return;
        }
        if (!StringUtils.hasText(sqsProperties.getFittingResponseQueueUrl())) {
            log.warn("BE_FITTING_RESPONSE_QUEUE_URL이 설정되지 않아 결과 소비를 건너뜁니다.");
            return;
        }

        List<Message> messages = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                        .queueUrl(sqsProperties.getFittingResponseQueueUrl())
                        .maxNumberOfMessages(properties.getMaxMessages())
                        .waitTimeSeconds(properties.getWaitTimeSeconds())
                        .visibilityTimeout(properties.getVisibilityTimeoutSeconds())
                        .build())
                .messages();
        messages.forEach(this::consumeOne);
    }

    private void consumeOne(Message message) {
        try {
            FittingResultMessage resultMessage = objectMapper.readValue(
                    message.body(), FittingResultMessage.class
            );
            Optional<FittingNotificationCreatedEvent> notificationEvent =
                    messageHandler.handle(resultMessage);

            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(sqsProperties.getFittingResponseQueueUrl())
                    .receiptHandle(message.receiptHandle())
                    .build());
            notificationEvent.ifPresent(eventPublisher::publishEvent);
        } catch (Exception exception) {
            log.error("FITTING_RESULT_CONSUME_FAILED messageId={}",
                    message.messageId(), exception);
        }
    }
}
