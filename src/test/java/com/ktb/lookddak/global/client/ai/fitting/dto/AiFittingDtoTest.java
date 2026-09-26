package com.ktb.lookddak.global.client.ai.fitting.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiFittingDtoTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    @DisplayName("AI 가상피팅 요청을 snake_case JSON으로 직렬화한다")
    void serializeRequest() throws Exception {
        AiFittingRequest request = new AiFittingRequest(
                "https://s3.example.com/user-image.png?signature=test",
                List.of(
                        new AiFittingProductRequest("1234333"),
                        new AiFittingProductRequest("5678111")
                )
        );

        String json = objectMapper.writeValueAsString(request);

        assertThat(json).isEqualTo(
                "{\"user_image_url\":"
                        + "\"https://s3.example.com/user-image.png?signature=test\","
                        + "\"products\":[{\"product_code\":\"1234333\"},"
                        + "{\"product_code\":\"5678111\"}]}"
        );
    }

    @Test
    @DisplayName("AI 가상피팅 성공 응답을 역직렬화한다")
    void deserializeSuccessResponse() throws Exception {
        AiFittingResponse response = objectMapper.readValue(
                """
                        {
                          "code": 200,
                          "message": "fitting_succeeded",
                          "data": {
                            "result_image_key": "virtual-fitting/results/result.png",
                            "llm_title": "차분한 미니멀 데이트룩",
                            "llm_comment": "깔끔한 색감이 어우러지는 코디입니다."
                          }
                        }
                        """,
                AiFittingResponse.class
        );

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getMessage()).isEqualTo("fitting_succeeded");
        assertThat(response.getData().getResultImageKey())
                .isEqualTo("virtual-fitting/results/result.png");
        assertThat(response.getData().getLlmTitle())
                .isEqualTo("차분한 미니멀 데이트룩");
        assertThat(response.getData().getLlmComment())
                .isEqualTo("깔끔한 색감이 어우러지는 코디입니다.");
    }

    @Test
    @DisplayName("AI 오류 응답의 data가 null이어도 응답을 역직렬화한다")
    void deserializeErrorResponse() throws Exception {
        AiFittingResponse response = objectMapper.readValue(
                """
                        {
                          "code": 500,
                          "message": "internal_server_error",
                          "data": null
                        }
                        """,
                AiFittingResponse.class
        );

        assertThat(response.getCode()).isEqualTo(500);
        assertThat(response.getMessage()).isEqualTo("internal_server_error");
        assertThat(response.getData()).isNull();
    }
}
