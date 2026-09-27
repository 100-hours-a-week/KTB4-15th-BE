package com.ktb.lookddak.global.client.ai.bodyimage.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class AiBodyImageValidationDtoTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    @DisplayName("AI 전신사진 검증 성공 응답을 역직렬화한다")
    void deserializeSuccessResponse() throws Exception {
        AiBodyImageValidationResponse response = objectMapper.readValue(
                """
                        {
                          "code": 200,
                          "message": "body_image_validation_success",
                          "data": {
                            "s3_key": "users/7/body-images/example.png",
                            "warnings": ["IMAGE_TOO_DARK"]
                          }
                        }
                        """,
                AiBodyImageValidationResponse.class
        );

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getMessage())
                .isEqualTo("body_image_validation_success");
        assertThat(response.getData().getS3Key())
                .isEqualTo("users/7/body-images/example.png");
        assertThat(response.getData().getWarnings())
                .containsExactly("IMAGE_TOO_DARK");
    }

    @Test
    @DisplayName("warnings가 없으면 빈 목록으로 변환한다")
    void deserializeSuccessResponseWithoutWarnings() throws Exception {
        AiBodyImageValidationResponse response = objectMapper.readValue(
                """
                        {
                          "code": 200,
                          "message": "body_image_validation_success",
                          "data": {
                            "s3_key": "users/7/body-images/example.png"
                          }
                        }
                        """,
                AiBodyImageValidationResponse.class
        );

        assertThat(response.getData().getWarnings()).isEmpty();
    }

    @Test
    @DisplayName("AI 전신사진 사용자 검증 실패 응답을 역직렬화한다")
    void deserializeUserValidationErrorResponse() throws Exception {
        AiBodyImageValidationErrorResponse response = objectMapper.readValue(
                """
                        {
                          "code": 422,
                          "message": "body_image_validation_failed",
                          "data": {
                            "reason_code": "FULL_BODY_NOT_VISIBLE",
                            "reason": "머리부터 발끝까지 모두 나오도록 전신을 촬영해주세요."
                          }
                        }
                        """,
                AiBodyImageValidationErrorResponse.class
        );

        assertThat(response.getCode()).isEqualTo(422);
        assertThat(response.getMessage())
                .isEqualTo("body_image_validation_failed");
        assertThat(response.getData().getReasonCode())
                .isEqualTo("FULL_BODY_NOT_VISIBLE");
        assertThat(response.getData().getReason())
                .isEqualTo("머리부터 발끝까지 모두 나오도록 전신을 촬영해주세요.");
    }

    @Test
    @DisplayName("상세 정보가 없는 AI 시스템 오류도 역직렬화한다")
    void deserializeErrorResponseWithoutData() throws Exception {
        AiBodyImageValidationErrorResponse response = objectMapper.readValue(
                """
                        {
                          "code": 500,
                          "message": "internal_server_error",
                          "data": null
                        }
                        """,
                AiBodyImageValidationErrorResponse.class
        );

        assertThat(response.getData()).isNull();
    }
}
