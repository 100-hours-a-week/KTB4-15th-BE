package com.ktb.lookddak.domain.image.service;

import com.ktb.lookddak.domain.image.dto.BodyImageWarmupResponse;
import com.ktb.lookddak.global.client.ai.bodyimage.AiBodyImageWarmupClient;
import com.ktb.lookddak.global.client.ai.exception.AiClientException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class BodyImageWarmupServiceTest {

    @Mock
    private AiBodyImageWarmupClient warmupClient;

    @Test
    @DisplayName("AI가 접수하면 202 결과를 반환한다")
    void returnAcceptedWhenAiAcceptsRequest() {
        given(warmupClient.requestWarmup()).willReturn(
                new AiBodyImageWarmupClient.AiBodyImageWarmupResult(
                        "BODY_IMAGE_WARMUP_ACCEPTED",
                        "전신 이미지 검증 사전 활성화 요청을 접수했습니다."
                )
        );

        BodyImageWarmupResponse response = service().requestWarmup();

        assertThat(response.status()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.code()).isEqualTo("BODY_IMAGE_WARMUP_ACCEPTED");
    }

    @Test
    @DisplayName("AI가 호출을 제한하면 Retry-After를 포함한 429 결과를 반환한다")
    void returnRateLimitedWhenAiRateLimitsRequest() {
        given(warmupClient.requestWarmup()).willThrow(new AiClientException(
                "BODY_IMAGE_WARMUP_RATE_LIMITED",
                "rate limited",
                429,
                12L,
                null
        ));

        BodyImageWarmupResponse response = service().requestWarmup();

        assertThat(response.status()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.code()).isEqualTo("BODY_IMAGE_WARMUP_RATE_LIMITED");
        assertThat(response.retryAfterSeconds()).isEqualTo(12L);
    }

    @Test
    @DisplayName("AI 연결 실패는 Warm-up 전용 502 결과로 변환한다")
    void returnUnavailableWhenAiIsUnreachable() {
        given(warmupClient.requestWarmup()).willThrow(new AiClientException(
                "AI_SERVER_UNAVAILABLE",
                "unavailable",
                null
        ));

        BodyImageWarmupResponse response = service().requestWarmup();

        assertThat(response.status()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.code()).isEqualTo("BODY_IMAGE_WARMUP_UNAVAILABLE");
    }

    @Test
    @DisplayName("AI 내부 인증 실패는 FE에 노출하지 않고 503으로 변환한다")
    void hideAiInternalAuthenticationFailure() {
        given(warmupClient.requestWarmup()).willThrow(new AiClientException(
                "UNAUTHORIZED",
                "AI internal key is invalid",
                401
        ));

        BodyImageWarmupResponse response = service().requestWarmup();

        assertThat(response.status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.code()).isEqualTo("BODY_IMAGE_WARMUP_UNAVAILABLE");
    }

    private BodyImageWarmupService service() {
        return new BodyImageWarmupService(warmupClient);
    }
}
