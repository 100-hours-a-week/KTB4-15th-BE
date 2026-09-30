package com.ktb.lookddak.global.client.ai.bodyimage;

import com.ktb.lookddak.domain.image.exception.BodyImageValidationException;
import com.ktb.lookddak.global.client.ai.exception.AiClientException;
import com.ktb.lookddak.global.exception.BusinessException;
import com.ktb.lookddak.global.exception.ErrorCode;
import com.ktb.lookddak.global.exception.ExternalApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiBodyImageValidationErrorMapperTest {

    private final AiBodyImageValidationErrorMapper mapper =
            new AiBodyImageValidationErrorMapper();

    @Test
    @DisplayName("AI 사용자 검증 코드와 메시지를 별도 변환 없이 보존한다")
    void preserveUserValidationError() {
        RuntimeException mapped = mapper.map(new AiClientException(
                "NEW_BODY_IMAGE_REASON",
                "새롭게 추가된 사용자 안내 메시지입니다.",
                422
        ));

        assertThat(mapped).isInstanceOfSatisfying(
                BodyImageValidationException.class,
                exception -> {
                    assertThat(exception.getCode())
                            .isEqualTo("NEW_BODY_IMAGE_REASON");
                    assertThat(exception.getMessage())
                            .isEqualTo("새롭게 추가된 사용자 안내 메시지입니다.");
                }
        );
    }

    @Test
    @DisplayName("AI 연결 실패와 응답 시간 초과를 백엔드 오류로 변환한다")
    void mapCommunicationErrors() {
        assertExternalApiError(
                mapper.map(exception("AI_SERVER_UNAVAILABLE")),
                ErrorCode.BODY_IMAGE_AI_SERVER_UNAVAILABLE
        );
        assertExternalApiError(
                mapper.map(exception("AI_RESPONSE_TIMEOUT")),
                ErrorCode.BODY_IMAGE_VALIDATION_TIMEOUT
        );
    }

    @Test
    @DisplayName("AI 처리량 초과를 429 비즈니스 오류로 변환한다")
    void mapServerBusy() {
        RuntimeException mapped = mapper.map(
                new AiClientException(
                        "SERVER_BUSY",
                        "현재 이미지 처리 요청이 많습니다. 잠시 후 다시 시도해주세요.",
                        429
                )
        );

        assertThat(mapped).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.SERVER_BUSY)
        );
    }

    @Test
    @DisplayName("AI 내부 오류와 불완전한 검증 오류는 공통 시스템 오류로 변환한다")
    void mapInternalErrorToSystemError() {
        assertExternalApiError(
                mapper.map(new AiClientException(
                        "BODY_IMAGE_SYSTEM_FAILED",
                        "AI system error",
                        500
                )),
                ErrorCode.BODY_IMAGE_VALIDATION_SYSTEM_ERROR
        );
    }

    private void assertExternalApiError(
            RuntimeException mapped,
            ErrorCode expectedErrorCode
    ) {
        assertThat(mapped).isInstanceOfSatisfying(
                ExternalApiException.class,
                exception -> {
                    assertThat(exception.getErrorCode())
                            .isEqualTo(expectedErrorCode);
                    assertThat(exception.getCause())
                            .isInstanceOf(AiClientException.class);
                }
        );
    }

    private AiClientException exception(String code) {
        return new AiClientException(
                code,
                "AI error",
                null
        );
    }
}
