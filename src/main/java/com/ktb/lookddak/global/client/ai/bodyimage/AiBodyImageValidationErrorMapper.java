package com.ktb.lookddak.global.client.ai.bodyimage;

import com.ktb.lookddak.domain.image.exception.BodyImageValidationException;
import com.ktb.lookddak.global.client.ai.bodyimage.exception.AiBodyImageValidationException;
import com.ktb.lookddak.global.exception.ErrorCode;
import com.ktb.lookddak.global.exception.ExternalApiException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AiBodyImageValidationErrorMapper {

    public RuntimeException map(AiBodyImageValidationException exception) {
        if ("AI_RESPONSE_TIMEOUT".equals(exception.getCode())) {
            return new ExternalApiException(
                    ErrorCode.BODY_IMAGE_VALIDATION_TIMEOUT,
                    exception
            );
        }
        if ("AI_SERVER_UNAVAILABLE".equals(exception.getCode())) {
            return new ExternalApiException(
                    ErrorCode.BODY_IMAGE_AI_SERVER_UNAVAILABLE,
                    exception
            );
        }

        if (isUserValidationError(exception)) {
            return new BodyImageValidationException(
                    exception.getReasonCode(),
                    exception.getReason()
            );
        }

        return new ExternalApiException(
                ErrorCode.BODY_IMAGE_VALIDATION_SYSTEM_ERROR,
                exception
        );
    }

    private boolean isUserValidationError(
            AiBodyImageValidationException exception
    ) {
        Integer status = exception.getHttpStatus();
        return "body_image_validation_failed".equals(exception.getCode())
                && status != null
                && (status == 400 || status == 413 || status == 422)
                && StringUtils.hasText(exception.getReasonCode())
                && StringUtils.hasText(exception.getReason());
    }
}
