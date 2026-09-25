package com.ktb.lookddak.global.exception;

import com.ktb.lookddak.global.response.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler();

    @Test
    @DisplayName("multipart 요청이 제한 크기를 초과하면 413 응답을 반환한다")
    void handleMaxUploadSizeExceeded() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleMaxUploadSizeExceeded(
                        new MaxUploadSizeExceededException(
                                10L * 1024 * 1024,
                                new IllegalStateException("upload too large")
                        )
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.CONTENT_TOO_LARGE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("IMAGE_TOO_LARGE");
        assertThat(response.getBody().getData()).isNull();
        assertThat(response.getBody().getMessage())
                .isEqualTo("이미지 크기는 10MB 이하여야 합니다.");
    }
}
