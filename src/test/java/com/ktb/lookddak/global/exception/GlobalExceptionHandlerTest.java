package com.ktb.lookddak.global.exception;

import com.ktb.lookddak.domain.image.exception.BodyImageValidationException;
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

    @Test
    @DisplayName("AI 사용자 검증 코드와 메시지를 공통 응답 형식으로 반환한다")
    void handleBodyImageValidation() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleBodyImageValidation(
                        new BodyImageValidationException(
                                "FULL_BODY_NOT_VISIBLE",
                                "머리부터 발끝까지 모두 나오도록 전신을 촬영해주세요."
                        )
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode())
                .isEqualTo("FULL_BODY_NOT_VISIBLE");
        assertThat(response.getBody().getData()).isNull();
        assertThat(response.getBody().getMessage())
                .isEqualTo("머리부터 발끝까지 모두 나오도록 전신을 촬영해주세요.");
    }

    @Test
    @DisplayName("외부 API 오류는 정제된 백엔드 오류 응답으로 반환한다")
    void handleExternalApiException() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleExternalApiException(
                        new ExternalApiException(
                                ErrorCode.BODY_IMAGE_AI_SERVER_UNAVAILABLE,
                                new IllegalStateException("internal AI detail")
                        )
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode())
                .isEqualTo("BODY_IMAGE_AI_SERVER_UNAVAILABLE");
        assertThat(response.getBody().getData()).isNull();
        assertThat(response.getBody().getMessage())
                .isEqualTo("전신사진 검증 서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.");
    }
}
