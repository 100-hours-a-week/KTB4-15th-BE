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

    @Test
    @DisplayName("회원 기본정보가 없으면 404 오류 응답으로 변환된다")
    void memberProfileNotFound() {
        ErrorCode errorCode = ErrorCode.MEMBER_PROFILE_NOT_FOUND;
        ApiResponse<Void> response = ApiResponse.failure(errorCode);

        assertThat(errorCode.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getCode()).isEqualTo("MEMBER_PROFILE_NOT_FOUND");
        assertThat(response.getData()).isNull();
        assertThat(response.getMessage())
                .isEqualTo("회원 기본정보를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("가상피팅 작업 오류는 상황에 맞는 HTTP 상태로 변환된다")
    void fittingJobErrors() {
        assertThat(ErrorCode.FITTING_PRODUCT_REQUIRED.getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ErrorCode.FITTING_PRODUCT_TYPE_MISMATCH.getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ErrorCode.FITTING_PRODUCT_NOT_CANDIDATE.getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ErrorCode.FITTING_JOB_ALREADY_GENERATING.getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(ErrorCode.FITTING_JOB_NOT_FOUND.getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ErrorCode.FITTING_JOB_ACCESS_DENIED.getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }
}
