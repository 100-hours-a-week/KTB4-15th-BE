package com.ktb.lookddak.global.client.ai.exception;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class AiFailureClassifierTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "AI_RESPONSE_TIMEOUT",
            "AI_SERVER_UNAVAILABLE",
            "AI_INVALID_RESPONSE",
            "AI_HTTP_ERROR",
            "AI_STREAM_CLOSED",
            "AI_CHAT_ID_MISMATCH"
    })
    void 운영_장애는_알림_대상이다(String code) {
        assertThat(AiFailureClassifier.requiresIncidentAlert(code)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "SERVER_BUSY",
            "INVALID_REQUEST",
            "PRODUCT_NOT_FOUND"
    })
    void 업무상_실패는_알림_대상이_아니다(String code) {
        assertThat(AiFailureClassifier.requiresIncidentAlert(code)).isFalse();
    }
}
