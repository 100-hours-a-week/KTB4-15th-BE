package com.ktb.lookddak.global.client.ai.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AiClientConfigTest {

    private final AiClientConfig config = new AiClientConfig();

    @Test
    @DisplayName("API Key가 있으면 AI 요청에 Bearer 인증 헤더를 추가한다")
    void addBearerAuthorizationHeader() {
        AtomicReference<ClientRequest> capturedRequest = new AtomicReference<>();
        WebClient webClient = config.aiWebClient(
                capturingBuilder(capturedRequest),
                properties("internal-key")
        );

        webClient.get()
                .uri("/health")
                .retrieve()
                .toBodilessEntity()
                .block();

        ClientRequest request = capturedRequest.get();
        assertThat(request.url().toString())
                .isEqualTo("http://localhost:8000/health");
        assertThat(request.headers().getFirst(HttpHeaders.AUTHORIZATION))
                .isEqualTo("Bearer internal-key");
    }

    @Test
    @DisplayName("API Key가 비어 있으면 Authorization 헤더를 추가하지 않는다")
    void omitAuthorizationHeaderWithoutApiKey() {
        AtomicReference<ClientRequest> capturedRequest = new AtomicReference<>();
        WebClient webClient = config.aiWebClient(
                capturingBuilder(capturedRequest),
                properties("")
        );

        webClient.get()
                .uri("/health")
                .retrieve()
                .toBodilessEntity()
                .block();

        assertThat(capturedRequest.get().headers()
                .getFirst(HttpHeaders.AUTHORIZATION)).isNull();
    }

    private WebClient.Builder capturingBuilder(
            AtomicReference<ClientRequest> capturedRequest
    ) {
        return WebClient.builder().exchangeFunction(request -> {
            capturedRequest.set(request);
            return Mono.just(ClientResponse.create(HttpStatus.OK).build());
        });
    }

    private AiClientProperties properties(String apiKey) {
        return new AiClientProperties(
                "http://localhost:8000",
                apiKey,
                Duration.ofSeconds(3),
                Duration.ofSeconds(60)
        );
    }
}
