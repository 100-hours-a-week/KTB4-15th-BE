package com.ktb.lookddak.domain.fitting.mq.message;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FittingMessageTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    void serializeRequestWithFittingJobIdAndImageKey() {
        FittingRequestMessage message = new FittingRequestMessage(
                123L,
                "2026-10-18T06:00:00Z",
                "members/1/full-body.png",
                List.of(
                        new FittingRequestProductMessage("000001"),
                        new FittingRequestProductMessage("000002")
                )
        );

        String json = objectMapper.writeValueAsString(message);

        assertThat(json).isEqualTo("""
                {"fitting_job_id":123,"requested_at":"2026-10-18T06:00:00Z","user_image_key":"members/1/full-body.png","products":[{"product_code":"000001"},{"product_code":"000002"}]}
                """.strip());
    }

    @Test
    void deserializeCompletedResultWithFittingJobId() {
        FittingResultMessage message = objectMapper.readValue("""
                {
                  "fitting_job_id": 123,
                  "status": "COMPLETED",
                  "result_image_key": "fitting/results/123.png",
                  "llm_title": "출근 니트 룩",
                  "llm_comment": "차분한 출근 코디입니다."
                }
                """, FittingResultMessage.class);

        assertThat(message.getFittingJobId()).isEqualTo(123L);
        assertThat(message.getStatus()).isEqualTo(FittingResultStatus.COMPLETED);
        assertThat(message.getResultImageKey()).isEqualTo("fitting/results/123.png");
        assertThat(message.getErrorCode()).isNull();
    }

    @Test
    void deserializeFailedResultWithFittingJobId() {
        FittingResultMessage message = objectMapper.readValue("""
                {
                  "fitting_job_id": 123,
                  "status": "FAILED",
                  "error_code": "FITTING_MODEL_FAILED",
                  "error_message": "가상피팅 이미지 생성에 실패했습니다."
                }
                """, FittingResultMessage.class);

        assertThat(message.getFittingJobId()).isEqualTo(123L);
        assertThat(message.getStatus()).isEqualTo(FittingResultStatus.FAILED);
        assertThat(message.getResultImageKey()).isNull();
        assertThat(message.getErrorCode()).isEqualTo("FITTING_MODEL_FAILED");
    }
}
