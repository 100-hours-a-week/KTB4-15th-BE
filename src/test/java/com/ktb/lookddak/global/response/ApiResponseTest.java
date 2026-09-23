package com.ktb.lookddak.global.response;

import com.ktb.lookddak.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    @DisplayName("성공 응답을 생성한다")
    void createSuccessResponse() {
        String data = "result";

        ApiResponse<String> response = ApiResponse.success(SuccessCode.OK, data);

        assertThat(response.getCode()).isEqualTo("OK");
        assertThat(response.getData()).isEqualTo(data);
        assertThat(response.getMessage()).isEqualTo("요청이 성공적으로 처리되었습니다.");
    }

    @Test
    @DisplayName("실패 응답은 데이터를 null로 반환한다")
    void createFailureResponse() {
        ApiResponse<Void> response = ApiResponse.failure(ErrorCode.INVALID_INPUT_VALUE);

        assertThat(response.getCode()).isEqualTo("INVALID_INPUT_VALUE");
        assertThat(response.getData()).isNull();
        assertThat(response.getMessage()).isEqualTo("입력값이 올바르지 않습니다.");
    }
}
