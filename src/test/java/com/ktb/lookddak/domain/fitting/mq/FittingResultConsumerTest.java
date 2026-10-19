package com.ktb.lookddak.domain.fitting.mq;

import com.ktb.lookddak.domain.fitting.mq.message.FittingResultMessage;
import com.ktb.lookddak.domain.fitting.mq.message.FittingResultStatus;
import com.ktb.lookddak.domain.notification.entity.NotificationType;
import com.ktb.lookddak.domain.notification.event.FittingNotificationCreatedEvent;
import com.ktb.lookddak.global.aws.sqs.SqsProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FittingResultConsumerTest {

    @Mock
    private SqsClient sqsClient;
    @Mock
    private FittingResultMessageHandler messageHandler;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    void deleteMessageThenPublishNotificationAfterSuccessfulPersistence() throws Exception {
        FittingNotificationCreatedEvent notificationEvent =
                new FittingNotificationCreatedEvent(
                        3L, 4L, 10L, NotificationType.FITTING_SUCCESS
                );
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder()
                .messages(Message.builder()
                        .messageId("message-1")
                        .receiptHandle("receipt-handle")
                        .body(new JsonMapper().writeValueAsString(new FittingResultMessage(
                                10L, FittingResultStatus.COMPLETED,
                                "fitting/result.png", "가을 룩", "설명", null, null
                        )))
                        .build())
                .build());
        when(messageHandler.handle(any())).thenReturn(Optional.of(notificationEvent));

        consumer(true).consume();

        ArgumentCaptor<DeleteMessageRequest> deleteCaptor =
                ArgumentCaptor.forClass(DeleteMessageRequest.class);
        verify(sqsClient).deleteMessage(deleteCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(deleteCaptor.getValue().receiptHandle())
                .isEqualTo("receipt-handle");
        verify(eventPublisher).publishEvent(notificationEvent);
    }

    @Test
    void doesNotPollWhenConsumerIsDisabled() {
        consumer(false).consume();

        verify(sqsClient, never()).receiveMessage(any(ReceiveMessageRequest.class));
    }

    private FittingResultConsumer consumer(boolean enabled) {
        return new FittingResultConsumer(
                sqsClient,
                new SqsProperties("ap-northeast-2", "", "queue-url"),
                new FittingResultConsumerProperties(enabled, 10, 0, 60),
                new JsonMapper(),
                messageHandler,
                eventPublisher
        );
    }
}
