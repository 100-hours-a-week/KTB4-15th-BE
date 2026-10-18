package com.ktb.lookddak.domain.fitting.mq.outbox;

import com.ktb.lookddak.global.aws.sqs.SqsProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublisherTest {

    @Mock
    private SqsClient sqsClient;

    @Mock
    private OutboxEventPublicationService publicationService;

    @Test
    void sendRequestMessageAndMarkEventPublished() {
        OutboxEventPublisher publisher = publisherWithQueueUrl();
        ClaimedOutboxEvent event = event(3);

        publisher.publish(event);

        ArgumentCaptor<SendMessageRequest> requestCaptor =
                ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsClient).sendMessage(requestCaptor.capture());
        assertThat(requestCaptor.getValue().queueUrl())
                .isEqualTo("https://sqs.example.com/be-fitting-request");
        assertThat(requestCaptor.getValue().messageBody())
                .isEqualTo("{\"fitting_job_id\":123}");
        verify(publicationService).markPublished(10L, "server-a");
    }

    @Test
    void returnEventToPendingWhenSqsSendFails() {
        OutboxEventPublisher publisher = publisherWithQueueUrl();
        ClaimedOutboxEvent event = event(3);
        doThrow(new IllegalStateException("SQS unavailable"))
                .when(sqsClient).sendMessage(any(SendMessageRequest.class));
        LocalDateTime beforePublish = LocalDateTime.now();

        publisher.publish(event);

        ArgumentCaptor<LocalDateTime> retryAtCaptor = ArgumentCaptor.forClass(
                LocalDateTime.class
        );
        verify(publicationService).markPublishFailed(
                eq(10L),
                eq("server-a"),
                retryAtCaptor.capture(),
                eq("SQS unavailable")
        );
        assertThat(retryAtCaptor.getValue())
                .isBetween(beforePublish.plusSeconds(8), beforePublish.plusSeconds(9));
    }

    @Test
    void recordFailureWhenRequestQueueUrlIsMissing() {
        OutboxEventPublisher publisher = new OutboxEventPublisher(
                sqsClient,
                new SqsProperties("ap-northeast-2", "", ""),
                publicationService
        );

        publisher.publish(event(0));

        verify(publicationService).markPublishFailed(
                eq(10L),
                eq("server-a"),
                any(LocalDateTime.class),
                eq("BE_FITTING_REQUEST_QUEUE_URL이 설정되지 않았습니다.")
        );
    }

    private OutboxEventPublisher publisherWithQueueUrl() {
        return new OutboxEventPublisher(
                sqsClient,
                new SqsProperties(
                        "ap-northeast-2",
                        "https://sqs.example.com/be-fitting-request",
                        ""
                ),
                publicationService
        );
    }

    private ClaimedOutboxEvent event(int retryCount) {
        return new ClaimedOutboxEvent(
                10L,
                123L,
                "{\"fitting_job_id\":123}",
                "server-a",
                retryCount
        );
    }
}
