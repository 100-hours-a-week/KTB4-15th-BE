package com.ktb.lookddak.global.client.ai.bodyimage;

import com.ktb.lookddak.global.client.ai.bodyimage.dto.AiBodyImageValidationData;
import com.ktb.lookddak.global.client.ai.bodyimage.dto.AiBodyImageValidationErrorData;
import com.ktb.lookddak.global.client.ai.bodyimage.dto.AiBodyImageValidationErrorResponse;
import com.ktb.lookddak.global.client.ai.bodyimage.dto.AiBodyImageValidationResponse;
import com.ktb.lookddak.global.client.ai.bodyimage.exception.AiBodyImageValidationException;
import com.ktb.lookddak.global.client.ai.config.AiClientProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.TimeoutException;

@Component
public class AiBodyImageValidationClient {

    private static final String VALIDATION_PATH =
            "/api/v1/validation/body-image";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final AiClientProperties properties;

    public AiBodyImageValidationClient(
            @Qualifier("aiWebClient") WebClient webClient,
            ObjectMapper objectMapper,
            AiClientProperties properties
    ) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public String validate(Long memberId, MultipartFile image) {
        MultiValueMap<String, HttpEntity<?>> multipartBody =
                createMultipartBody(memberId, image);

        AiBodyImageValidationResponse response = webClient.post()
                .uri(VALIDATION_PATH)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromMultipartData(multipartBody))
                .retrieve()
                .onStatus(status -> status.isError(), clientResponse -> {
                    int status = clientResponse.statusCode().value();
                    return clientResponse.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .map(body -> parseErrorResponse(status, body));
                })
                .bodyToMono(AiBodyImageValidationResponse.class)
                .timeout(properties.getResponseTimeout())
                .onErrorMap(
                        TimeoutException.class,
                        this::createTimeoutException
                )
                .onErrorMap(
                        WebClientRequestException.class,
                        this::createRequestException
                )
                .block();

        return getS3Key(response);
    }

    MultiValueMap<String, HttpEntity<?>> createMultipartBody(
            Long memberId,
            MultipartFile image
    ) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("user_id", memberId.toString());
        builder.part("image", image.getResource())
                .filename(image.getOriginalFilename() == null
                        ? "body-image"
                        : image.getOriginalFilename())
                .contentType(resolveContentType(image));
        return builder.build();
    }

    private MediaType resolveContentType(MultipartFile image) {
        String contentType = image.getContentType();
        return StringUtils.hasText(contentType)
                ? MediaType.parseMediaType(contentType)
                : MediaType.APPLICATION_OCTET_STREAM;
    }

    private AiBodyImageValidationException parseErrorResponse(
            int httpStatus,
            String body
    ) {
        if (!StringUtils.hasText(body)) {
            return new AiBodyImageValidationException(
                    "AI_HTTP_ERROR",
                    "AI 서버가 오류를 반환했습니다.",
                    httpStatus,
                    null,
                    null
            );
        }

        try {
            AiBodyImageValidationErrorResponse response =
                    objectMapper.readValue(
                            body,
                            AiBodyImageValidationErrorResponse.class
                    );
            AiBodyImageValidationErrorData data = response.getData();
            return new AiBodyImageValidationException(
                    response.getMessage(),
                    "AI 전신사진 검증 요청이 실패했습니다.",
                    httpStatus,
                    data == null ? null : data.getReasonCode(),
                    data == null ? null : data.getReason()
            );
        } catch (JacksonException exception) {
            return new AiBodyImageValidationException(
                    "AI_INVALID_RESPONSE",
                    "AI 오류 응답 형식을 해석할 수 없습니다.",
                    httpStatus,
                    null,
                    null,
                    exception
            );
        }
    }

    private String getS3Key(AiBodyImageValidationResponse response) {
        AiBodyImageValidationData data = response == null
                ? null
                : response.getData();
        if (data == null || !StringUtils.hasText(data.getS3Key())) {
            throw new AiBodyImageValidationException(
                    "AI_INVALID_RESPONSE",
                    "AI 응답에 S3 객체 Key가 없습니다.",
                    null,
                    null,
                    null
            );
        }
        return data.getS3Key();
    }

    private AiBodyImageValidationException createTimeoutException(
            Throwable exception
    ) {
        return new AiBodyImageValidationException(
                "AI_RESPONSE_TIMEOUT",
                "AI 전신사진 검증 응답 시간을 초과했습니다.",
                null,
                null,
                null,
                exception
        );
    }

    private AiBodyImageValidationException createRequestException(
            WebClientRequestException exception
    ) {
        if (isTimeout(exception)) {
            return createTimeoutException(exception);
        }
        return new AiBodyImageValidationException(
                "AI_SERVER_UNAVAILABLE",
                "AI 서버에 연결할 수 없습니다.",
                null,
                null,
                null,
                exception
        );
    }

    private boolean isTimeout(Throwable exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof TimeoutException
                    || cause.getClass().getSimpleName().contains("Timeout")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
