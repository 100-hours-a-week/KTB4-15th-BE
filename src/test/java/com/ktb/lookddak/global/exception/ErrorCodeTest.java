package com.ktb.lookddak.global.exception;

import com.ktb.lookddak.global.response.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCodeTest {

    @Test
    @DisplayName("잘못된 페이지 조회 조건은 400 오류 응답으로 변환된다")
    void invalidPaginationParameter() {
        ErrorCode errorCode = ErrorCode.INVALID_PAGINATION_PARAMETER;
        ApiResponse<Void> response = ApiResponse.failure(errorCode);

        assertThat(errorCode.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getCode()).isEqualTo("INVALID_PAGINATION_PARAMETER");
        assertThat(response.getData()).isNull();
        assertThat(response.getMessage()).isEqualTo("올바른 조회 조건을 입력해주세요.");
    }
}
