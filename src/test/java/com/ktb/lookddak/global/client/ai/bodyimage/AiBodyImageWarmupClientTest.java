package com.ktb.lookddak.global.client.ai.bodyimage;

import com.ktb.lookddak.global.client.ai.config.AiClientProperties;
import com.ktb.lookddak.global.client.ai.exception.AiClientException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiBodyImageWarmupClientTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    @DisplayName("AI Warm-up 접수 API에 본문 없이 POST 요청한다")
    void requestWarmup() {
        AtomicReference<String> path = new AtomicReference<>();
        AiBodyImageWarmupClient client = createClient(request -> {
            path.set(request.url().getPath());
            return Mono.just(jsonResponse(HttpStatus.ACCEPTED, acceptedBody()));
        });

        AiBodyImageWarmupClient.AiBodyImageWarmupResult result =
                client.requestWarmup();

        assertThat(path.get())
                .isEqualTo("/api/v1/validation/body-image/warmup");
        assertThat(result.code()).isEqualTo("BODY_IMAGE_WARMUP_ACCEPTED");
    }

    @Test
    @DisplayName("AI의 429와 Retry-After 값을 보존한다")
    void preserveRateLimitResponse() {
        AiBodyImageWarmupClient client = createClient(ignored -> Mono.just(
                ClientResponse.create(HttpStatus.TOO_MANY_REQUESTS)
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .header("Retry-After", "12")
                        .body("""
                                {
                                  "code": "BODY_IMAGE_WARMUP_RATE_LIMITED",
                                  "data": null,
                                  "message": "사전 활성화 요청이 너무 많습니다."
                                }
                                """)
                        .build()
        ));

        assertThatThrownBy(client::requestWarmup)
                .isInstanceOfSatisfying(AiClientException.class, exception -> {
                    assertThat(exception.getHttpStatus()).isEqualTo(429);
                    assertThat(exception.getCode())
                            .isEqualTo("BODY_IMAGE_WARMUP_RATE_LIMITED");
                    assertThat(exception.getRetryAfterSeconds()).isEqualTo(12L);
                });
    }

    @Test
    @DisplayName("202라도 예상한 접수 code가 아니면 잘못된 응답으로 처리한다")
    void rejectUnexpectedAcceptedCode() {
        AiBodyImageWarmupClient client = createClient(
                ignored -> Mono.just(jsonResponse(HttpStatus.ACCEPTED, """
                        {
                          "code": "UNEXPECTED_CODE",
                          "data": null,
                          "message": "unexpected"
                        }
                        """))
        );

        assertThatThrownBy(client::requestWarmup)
                .isInstanceOfSatisfying(AiClientException.class, exception ->
                        assertThat(exception.getCode())
                                .isEqualTo("AI_INVALID_RESPONSE")
                );
    }

    private AiBodyImageWarmupClient createClient(
            ExchangeFunction exchangeFunction
    ) {
        WebClient webClient = WebClient.builder()
                .baseUrl("http://localhost:8000")
                .exchangeFunction(exchangeFunction)
                .build();
        AiClientProperties properties = new AiClientProperties(
                "http://localhost:8000",
                "internal-key",
                Duration.ofSeconds(10),
                Duration.ofSeconds(1)
        );
        return new AiBodyImageWarmupClient(
                webClient,
                objectMapper,
                properties
        );
    }

    private ClientResponse jsonResponse(HttpStatus status, String body) {
        return ClientResponse.create(status)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .build();
    }

    private String acceptedBody() {
        return """
                {
                  "code": "BODY_IMAGE_WARMUP_ACCEPTED",
                  "data": null,
                  "message": "전신 이미지 검증 사전 활성화 요청을 접수했습니다."
                }
                """;
    }
}
