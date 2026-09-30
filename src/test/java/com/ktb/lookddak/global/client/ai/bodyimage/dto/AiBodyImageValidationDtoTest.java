package com.ktb.lookddak.global.client.ai.bodyimage.dto;

import com.ktb.lookddak.global.client.ai.dto.AiErrorResponse;
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
                          "code": "BODY_IMAGE_UPLOAD_SUCCESS",
                          "data": {
                            "s3_key": "users/7/body-images/example.png"
                          },
                          "message": "전신 사진 검증에 성공했습니다."
                        }
                        """,
                AiBodyImageValidationResponse.class
        );

        assertThat(response.getCode())
                .isEqualTo("BODY_IMAGE_UPLOAD_SUCCESS");
        assertThat(response.getMessage())
                .isEqualTo("전신 사진 검증에 성공했습니다.");
        assertThat(response.getData().getS3Key())
                .isEqualTo("users/7/body-images/example.png");
    }

    @Test
    @DisplayName("AI 전신사진 사용자 검증 실패 응답을 역직렬화한다")
    void deserializeUserValidationErrorResponse() throws Exception {
        AiErrorResponse response = objectMapper.readValue(
                """
                        {
                          "code": "FULL_BODY_NOT_VISIBLE",
                          "data": null,
                          "message": "머리부터 발끝까지 모두 나오도록 전신을 촬영해주세요."
                        }
                        """,
                AiErrorResponse.class
        );

        assertThat(response.getCode()).isEqualTo("FULL_BODY_NOT_VISIBLE");
        assertThat(response.getMessage())
                .isEqualTo("머리부터 발끝까지 모두 나오도록 전신을 촬영해주세요.");
        assertThat(response.getData()).isNull();
    }

    @Test
    @DisplayName("상세 정보가 없는 AI 시스템 오류도 역직렬화한다")
    void deserializeErrorResponseWithoutData() throws Exception {
        AiErrorResponse response = objectMapper.readValue(
                """
                        {
                          "code": "INTERNAL_SERVER_ERROR",
                          "data": null,
                          "message": "서버 내부 오류가 발생했습니다."
                        }
                        """,
                AiErrorResponse.class
        );

        assertThat(response.getData()).isNull();
    }
}
