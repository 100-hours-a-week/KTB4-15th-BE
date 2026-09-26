package com.ktb.lookddak.global.client.ai.fitting;

import com.ktb.lookddak.global.client.ai.config.AiClientProperties;
import com.ktb.lookddak.global.client.ai.fitting.dto.AiFittingProductRequest;
import com.ktb.lookddak.global.client.ai.fitting.dto.AiFittingRequest;
import com.ktb.lookddak.global.client.ai.fitting.dto.AiFittingResultData;
import com.ktb.lookddak.global.client.ai.fitting.exception.AiFittingException;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiFittingClientTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    @DisplayName("AI 가상피팅 성공 응답의 결과 데이터를 반환한다")
    void returnFittingResult() {
        AiFittingClient client = createClient(
                jsonResponse(HttpStatus.OK, successBody()),
                Duration.ofSeconds(1)
        );

        AiFittingResultData result = client.requestFitting(request());

        assertThat(result.getResultImageKey())
                .isEqualTo("virtual-fitting/results/result.png");
        assertThat(result.getLlmTitle()).isEqualTo("데일리 룩");
        assertThat(result.getLlmComment()).isEqualTo("자연스러운 코디입니다.");
    }

    @Test
    @DisplayName("AI HTTP 오류의 상태와 메시지 코드를 보존한다")
    void preserveHttpError() {
        AiFittingClient client = createClient(
                jsonResponse(HttpStatus.NOT_FOUND, """
                        {
                          "code": 404,
                          "message": "product_not_found",
                          "data": null
                        }
                        """),
                Duration.ofSeconds(1)
        );

        assertThatThrownBy(() -> client.requestFitting(request()))
                .isInstanceOfSatisfying(AiFittingException.class, exception -> {
                    assertThat(exception.getCode())
                            .isEqualTo("product_not_found");
                    assertThat(exception.getHttpStatus()).isEqualTo(404);
                });
    }

    @Test
    @DisplayName("필수 결과값이 누락된 성공 응답을 거부한다")
    void rejectIncompleteSuccessResponse() {
        AiFittingClient client = createClient(
                jsonResponse(HttpStatus.OK, """
                        {
                          "code": 200,
                          "message": "fitting_succeeded",
                          "data": {
                            "result_image_key": "",
                            "llm_title": "데일리 룩",
                            "llm_comment": "코디 설명"
                          }
                        }
                        """),
                Duration.ofSeconds(1)
        );

        assertExceptionCode(client, "AI_INVALID_RESPONSE");
    }

    @Test
    @DisplayName("AI 응답 시간을 초과하면 타임아웃으로 처리한다")
    void rejectResponseTimeout() {
        AiFittingClient client = createClient(
                ignored -> Mono.never(),
                Duration.ofMillis(10)
        );

        assertExceptionCode(client, "AI_RESPONSE_TIMEOUT");
    }

    private void assertExceptionCode(
            AiFittingClient client,
            String expectedCode
    ) {
        assertThatThrownBy(() -> client.requestFitting(request()))
                .isInstanceOfSatisfying(
                        AiFittingException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo(expectedCode)
                );
    }

    private AiFittingClient createClient(
            ExchangeFunction exchangeFunction,
            Duration responseTimeout
    ) {
        WebClient webClient = WebClient.builder()
                .baseUrl("http://localhost:8000")
                .exchangeFunction(exchangeFunction)
                .build();
        AiClientProperties properties = new AiClientProperties(
                "http://localhost:8000",
                "internal-key",
                Duration.ofSeconds(3),
                responseTimeout
        );
        return new AiFittingClient(webClient, objectMapper, properties);
    }

    private ExchangeFunction jsonResponse(HttpStatus status, String body) {
        return request -> Mono.just(ClientResponse.create(status)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .build());
    }

    private AiFittingRequest request() {
        return new AiFittingRequest(
                "https://s3.example.com/user-image.png",
                List.of(new AiFittingProductRequest("1234333"))
        );
    }

    private String successBody() {
        return """
                {
                  "code": 200,
                  "message": "fitting_succeeded",
                  "data": {
                    "result_image_key": "virtual-fitting/results/result.png",
                    "llm_title": "데일리 룩",
                    "llm_comment": "자연스러운 코디입니다."
                  }
                }
                """;
    }
}
