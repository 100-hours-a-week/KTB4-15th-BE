package com.ktb.lookddak.global.client.ai.bodyimage;

import com.ktb.lookddak.global.client.ai.exception.AiClientException;
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
    @DisplayName("AI 사용자 검증 실패의 code와 message를 보존한다")
    void preserveAiValidationError() {
        AiBodyImageValidationClient client = createClient(
                jsonResponse(HttpStatus.UNPROCESSABLE_CONTENT, """
                        {
                          "code": "FULL_BODY_NOT_VISIBLE",
                          "data": null,
                          "message": "머리부터 발끝까지 모두 나오도록 전신을 촬영해주세요."
                        }
                        """),
                Duration.ofSeconds(1)
        );

        assertThatThrownBy(() -> client.validate(7L, image()))
                .isInstanceOfSatisfying(
                        AiClientException.class,
                        exception -> {
                            assertThat(exception.getHttpStatus()).isEqualTo(422);
                            assertThat(exception.getCode())
                                    .isEqualTo("FULL_BODY_NOT_VISIBLE");
                            assertThat(exception.getMessage())
                                    .isEqualTo("머리부터 발끝까지 모두 나오도록 전신을 촬영해주세요.");
                        }
                );
    }

    @Test
    @DisplayName("AI 서버 처리량 초과의 code와 HTTP 상태를 보존한다")
    void preserveServerBusyError() {
        AiBodyImageValidationClient client = createClient(
                jsonResponse(HttpStatus.TOO_MANY_REQUESTS, """
                        {
                          "code": "SERVER_BUSY",
                          "data": null,
                          "message": "현재 이미지 처리 요청이 많습니다. 잠시 후 다시 시도해주세요."
                        }
                        """),
                Duration.ofSeconds(1)
        );

        assertThatThrownBy(() -> client.validate(7L, image()))
                .isInstanceOfSatisfying(
                        AiClientException.class,
                        exception -> {
                            assertThat(exception.getCode())
                                    .isEqualTo("SERVER_BUSY");
                            assertThat(exception.getHttpStatus())
                                    .isEqualTo(429);
                        }
                );
    }

    @Test
    @DisplayName("AI 응답에 S3 Key가 없으면 잘못된 응답으로 처리한다")
    void rejectResponseWithoutS3Key() {
        AiBodyImageValidationClient client = createClient(
                jsonResponse(HttpStatus.OK, """
                        {
                          "code": "BODY_IMAGE_UPLOAD_SUCCESS",
                          "data": {"s3_key": ""},
                          "message": "전신 사진 검증에 성공했습니다."
                        }
                        """),
                Duration.ofSeconds(1)
        );

        assertExceptionCode(client, "AI_INVALID_RESPONSE");
    }

    @Test
    @DisplayName("성공 응답의 애플리케이션 코드가 다르면 거부한다")
    void rejectUnexpectedSuccessCode() {
        AiBodyImageValidationClient client = createClient(
                jsonResponse(HttpStatus.OK, """
                        {
                          "code": "UNEXPECTED_CODE",
                          "data": {
                            "s3_key": "users/7/body-images/example.png"
                          },
                          "message": "예상하지 못한 응답"
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
                        AiClientException.class,
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
                  "code": "BODY_IMAGE_UPLOAD_SUCCESS",
                  "data": {
                    "s3_key": "users/7/body-images/example.png"
                  },
                  "message": "전신 사진 검증에 성공했습니다."
                }
                """;
    }
}
