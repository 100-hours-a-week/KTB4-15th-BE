package com.ktb.lookddak.global.client.ai.exception;

import java.util.Set;

public final class AiFailureClassifier {

    private static final Set<String> OPERATIONAL_FAILURE_CODES = Set.of(
            "AI_RESPONSE_TIMEOUT",
            "AI_SERVER_UNAVAILABLE",
            "AI_INVALID_RESPONSE",
            "AI_HTTP_ERROR",
            "AI_STREAM_CLOSED",
            "AI_CHAT_ID_MISMATCH"
    );

    private AiFailureClassifier() {
    }

    public static boolean requiresIncidentAlert(String code) {
        return OPERATIONAL_FAILURE_CODES.contains(code);
    }
}
