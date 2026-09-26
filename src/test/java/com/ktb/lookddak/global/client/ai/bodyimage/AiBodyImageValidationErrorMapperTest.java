package com.ktb.lookddak.global.client.ai.bodyimage;

import com.ktb.lookddak.domain.image.exception.BodyImageValidationException;
import com.ktb.lookddak.global.client.ai.bodyimage.exception.AiBodyImageValidationException;
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
        RuntimeException mapped = mapper.map(new AiBodyImageValidationException(
                "body_image_validation_failed",
                "AI validation error",
                422,
                "NEW_BODY_IMAGE_REASON",
                "새롭게 추가된 사용자 안내 메시지입니다."
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
    @DisplayName("AI 내부 오류와 불완전한 검증 오류는 공통 시스템 오류로 변환한다")
    void mapInternalErrorToSystemError() {
        assertExternalApiError(
                mapper.map(new AiBodyImageValidationException(
                        "body_image_validation_system_failed",
                        "AI system error",
                        500,
                        "PERSON_DETECTION_FAILED",
                        null
                )),
                ErrorCode.BODY_IMAGE_VALIDATION_SYSTEM_ERROR
        );
        assertExternalApiError(
                mapper.map(new AiBodyImageValidationException(
                        "body_image_validation_failed",
                        "AI validation error",
                        422,
                        "FULL_BODY_NOT_VISIBLE",
                        null
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
                            .isInstanceOf(AiBodyImageValidationException.class);
                }
        );
    }

    private AiBodyImageValidationException exception(String code) {
        return new AiBodyImageValidationException(
                code,
                "AI error",
                null,
                null,
                null
        );
    }
}
