package com.ktb.lookddak.global.client.ai.bodyimage;

import com.ktb.lookddak.global.client.ai.bodyimage.exception.AiBodyImageValidationException;
import com.ktb.lookddak.global.client.ai.config.AiClientProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiBodyImageValidationClientTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    @DisplayName("회원 ID와 이미지를 multipart 요청 데이터로 구성한다")
    void createMultipartBody() {
        AiBodyImageValidationClient client = createClient(
                jsonResponse(HttpStatus.OK, successBody()),
                Duration.ofSeconds(1)
        );
        MockMultipartFile image = image();

        MultiValueMap<String, HttpEntity<?>> body =
                client.createMultipartBody(7L, image);

        assertThat(body.getFirst("user_id").getBody()).isEqualTo("7");
        HttpEntity<?> imagePart = body.getFirst("image");
        assertThat(imagePart).isNotNull();
        assertThat(imagePart.getBody()).isInstanceOf(Resource.class);
        assertThat(((Resource) imagePart.getBody()).getFilename())
                .isEqualTo("body.png");
        assertThat(imagePart.getHeaders().getContentType())
                .isEqualTo(MediaType.IMAGE_PNG);
    }

    @Test
    @DisplayName("AI 검증 성공 응답에서 S3 Key를 반환한다")
    void returnS3Key() {
        AiBodyImageValidationClient client = createClient(
                jsonResponse(HttpStatus.OK, successBody()),
                Duration.ofSeconds(1)
        );

        String s3Key = client.validate(7L, image());

        assertThat(s3Key)
                .isEqualTo("users/7/body-images/example.png");
    }

    @Test
    @DisplayName("AI 사용자 검증 실패의 reasonCode와 reason을 보존한다")
    void preserveAiValidationError() {
        AiBodyImageValidationClient client = createClient(
                jsonResponse(HttpStatus.UNPROCESSABLE_CONTENT, """
                        {
                          "code": 422,
                          "message": "body_image_validation_failed",
                          "data": {
                            "reason_code": "FULL_BODY_NOT_VISIBLE",
                            "reason": "머리부터 발끝까지 모두 나오도록 전신을 촬영해주세요."
                          }
                        }
                        """),
                Duration.ofSeconds(1)
        );

        assertThatThrownBy(() -> client.validate(7L, image()))
                .isInstanceOfSatisfying(
                        AiBodyImageValidationException.class,
                        exception -> {
                            assertThat(exception.getHttpStatus()).isEqualTo(422);
                            assertThat(exception.getCode())
                                    .isEqualTo("body_image_validation_failed");
                            assertThat(exception.getReasonCode())
                                    .isEqualTo("FULL_BODY_NOT_VISIBLE");
                            assertThat(exception.getReason())
                                    .isEqualTo("머리부터 발끝까지 모두 나오도록 전신을 촬영해주세요.");
                        }
                );
    }

    @Test
    @DisplayName("AI 응답에 S3 Key가 없으면 잘못된 응답으로 처리한다")
    void rejectResponseWithoutS3Key() {
        AiBodyImageValidationClient client = createClient(
                jsonResponse(HttpStatus.OK, """
                        {
                          "code": 200,
                          "message": "body_image_validation_success",
                          "data": {"s3_key": "", "warnings": []}
                        }
                        """),
                Duration.ofSeconds(1)
        );

        assertExceptionCode(client, "AI_INVALID_RESPONSE");
    }

    @Test
    @DisplayName("AI 응답 제한 시간을 초과하면 타임아웃으로 처리한다")
    void rejectResponseTimeout() {
        AiBodyImageValidationClient client = createClient(
                ignored -> Mono.never(),
                Duration.ofMillis(10)
        );

        assertExceptionCode(client, "AI_RESPONSE_TIMEOUT");
    }

    private void assertExceptionCode(
            AiBodyImageValidationClient client,
            String expectedCode
    ) {
        assertThatThrownBy(() -> client.validate(7L, image()))
                .isInstanceOfSatisfying(
                        AiBodyImageValidationException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo(expectedCode)
                );
    }

    private AiBodyImageValidationClient createClient(
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
        return new AiBodyImageValidationClient(
                webClient,
                objectMapper,
                properties
        );
    }

    private ExchangeFunction jsonResponse(HttpStatus status, String body) {
        return request -> Mono.just(ClientResponse.create(status)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .build());
    }

    private MockMultipartFile image() {
        return new MockMultipartFile(
                "image",
                "body.png",
                MediaType.IMAGE_PNG_VALUE,
                new byte[]{1, 2, 3}
        );
    }

    private String successBody() {
        return """
                {
                  "code": 200,
                  "message": "body_image_validation_success",
                  "data": {
                    "s3_key": "users/7/body-images/example.png",
                    "warnings": []
                  }
                }
                """;
    }
}
