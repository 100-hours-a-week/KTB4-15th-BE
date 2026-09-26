package com.ktb.lookddak.domain.fitting.event;

import com.ktb.lookddak.domain.fitting.command.FittingGenerationCommand;
import com.ktb.lookddak.domain.fitting.service.FittingGenerationQueryService;
import com.ktb.lookddak.global.client.ai.fitting.AiFittingClient;
import com.ktb.lookddak.global.client.ai.fitting.dto.AiFittingRequest;
import com.ktb.lookddak.global.client.ai.fitting.dto.AiFittingResultData;
import com.ktb.lookddak.global.client.ai.fitting.exception.AiFittingException;
import com.ktb.lookddak.global.storage.s3.S3PresignedUrlProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FittingGenerationEventListenerTest {

    @Mock
    private FittingGenerationQueryService queryService;

    @Mock
    private S3PresignedUrlProvider presignedUrlProvider;

    @Mock
    private AiFittingClient aiFittingClient;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private FittingGenerationEventListener listener;

    @Test
    @DisplayName("가상피팅 요청 정보를 AI 요청으로 변환하고 성공 이벤트를 발행한다")
    void publishSucceededEvent() {
        FittingGenerationCommand command = command();
        AiFittingResultData result = new AiFittingResultData(
                "fitting/results/result.png",
                "차분한 데일리 룩",
                "깔끔한 색감이 어우러지는 코디입니다."
        );
        given(queryService.createCommand(100L)).willReturn(command);
        given(presignedUrlProvider.createGetUrl("profiles/member-1.png"))
                .willReturn("https://presigned.example.com/member-1.png");
        given(aiFittingClient.requestFitting(any(AiFittingRequest.class)))
                .willReturn(result);

        listener.handle(new FittingGenerationRequestedEvent(100L));

        ArgumentCaptor<AiFittingRequest> requestCaptor =
                ArgumentCaptor.forClass(AiFittingRequest.class);
        verify(aiFittingClient).requestFitting(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getUserImageUrl())
                .isEqualTo("https://presigned.example.com/member-1.png");
        assertThat(requestCaptor.getValue().getProducts())
                .extracting(product -> product.getProductCode())
                .containsExactly("0000001", "0000002");

        ArgumentCaptor<FittingGenerationSucceededEvent> eventCaptor =
                ArgumentCaptor.forClass(
                        FittingGenerationSucceededEvent.class
                );
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getFittingJobId()).isEqualTo(100L);
        assertThat(eventCaptor.getValue().getResult()).isSameAs(result);
    }

    @Test
    @DisplayName("AI 통신 오류가 발생하면 가상피팅 실패 이벤트를 발행한다")
    void publishFailedEventForAiException() {
        given(queryService.createCommand(100L)).willReturn(command());
        given(presignedUrlProvider.createGetUrl("profiles/member-1.png"))
                .willReturn("https://presigned.example.com/member-1.png");
        given(aiFittingClient.requestFitting(any(AiFittingRequest.class)))
                .willThrow(new AiFittingException(
                        "AI_RESPONSE_TIMEOUT",
                        "AI 가상피팅 응답 시간을 초과했습니다.",
                        null
                ));

        listener.handle(new FittingGenerationRequestedEvent(100L));

        ArgumentCaptor<FittingGenerationFailedEvent> eventCaptor =
                ArgumentCaptor.forClass(FittingGenerationFailedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getFittingJobId()).isEqualTo(100L);
        assertThat(eventCaptor.getValue().getCode())
                .isEqualTo("AI_RESPONSE_TIMEOUT");
    }

    @Test
    @DisplayName("요청 정보 조회 오류도 공통 가상피팅 실패 이벤트로 변환한다")
    void publishFailedEventForUnexpectedException() {
        given(queryService.createCommand(100L))
                .willThrow(new IllegalStateException("missing job"));

        listener.handle(new FittingGenerationRequestedEvent(100L));

        ArgumentCaptor<FittingGenerationFailedEvent> eventCaptor =
                ArgumentCaptor.forClass(FittingGenerationFailedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getCode())
                .isEqualTo("AI_UNEXPECTED_ERROR");
    }

    private FittingGenerationCommand command() {
        return new FittingGenerationCommand(
                100L,
                "profiles/member-1.png",
                List.of("0000001", "0000002")
        );
    }
}
