package com.ktb.lookddak.global.client.ai.chat;

import com.ktb.lookddak.global.client.ai.config.AiClientProperties;
import com.ktb.lookddak.global.client.ai.dto.AiChatDoneResponse;
import com.ktb.lookddak.global.client.ai.dto.AiChatErrorResponse;
import com.ktb.lookddak.global.client.ai.dto.AiChatRequest;
import com.ktb.lookddak.global.client.ai.exception.AiChatException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.TimeoutException;

@Component
public class AiChatClient {

    private static final String CHAT_STREAM_PATH = "/api/v1/chat/stream";
    private static final String DONE_EVENT = "done";
    private static final String ERROR_EVENT = "error";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final AiClientProperties properties;

    public AiChatClient(
            @Qualifier("aiWebClient") WebClient webClient,
            ObjectMapper objectMapper,
            AiClientProperties properties
    ) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public AiChatDoneResponse requestChat(AiChatRequest request) {
        Flux<ServerSentEvent<String>> events = webClient.post()
                .uri(CHAT_STREAM_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(body -> new AiChatException(
                                        "AI_HTTP_ERROR",
                                        "AI 서버가 HTTP "
                                                + response.statusCode().value()
                                                + " 오류를 반환했습니다. "
                                                + body,
                                        request.getChatId()
                                ))
                )
                .bodyToFlux(new ParameterizedTypeReference<>() {
                });

        return events
                .<AiChatDoneResponse>handle((event, sink) -> {
                    if (DONE_EVENT.equals(event.event())) {
                        sink.next(parseDoneEvent(event.data(), request));
                        return;
                    }

                    if (ERROR_EVENT.equals(event.event())) {
                        sink.error(parseErrorEvent(event.data(), request));
                    }
                })
                .next()
                .switchIfEmpty(Mono.error(new AiChatException(
                        "AI_STREAM_CLOSED",
                        "AI 응답이 완료되기 전에 연결이 종료되었습니다.",
                        request.getChatId()
                )))
                .timeout(properties.getResponseTimeout())
                .onErrorMap(
                        TimeoutException.class,
                        exception -> new AiChatException(
                                "AI_RESPONSE_TIMEOUT",
                                "AI 응답 제한 시간을 초과했습니다.",
                                request.getChatId(),
                                exception
                        )
                )
                .block();
    }

    private AiChatDoneResponse parseDoneEvent(
            String data,
            AiChatRequest request
    ) {
        AiChatDoneResponse response = readValue(
                data,
                AiChatDoneResponse.class,
                request.getChatId()
        );

        validateChatId(request.getChatId(), response.getChatId());
        return response;
    }

    private AiChatException parseErrorEvent(
            String data,
            AiChatRequest request
    ) {
        AiChatErrorResponse response = readValue(
                data,
                AiChatErrorResponse.class,
                request.getChatId()
        );

        validateChatId(request.getChatId(), response.getChatId());
        return new AiChatException(
                response.getCode(),
                response.getMessage(),
                response.getChatId()
        );
    }

    private <T> T readValue(
            String data,
            Class<T> responseType,
            Long chatId
    ) {
        if (data == null || data.isBlank()) {
            throw new AiChatException(
                    "AI_INVALID_RESPONSE",
                    "AI 응답 데이터가 비어 있습니다.",
                    chatId
            );
        }

        try {
            return objectMapper.readValue(data, responseType);
        } catch (JacksonException exception) {
            throw new AiChatException(
                    "AI_INVALID_RESPONSE",
                    "AI 응답 형식을 해석할 수 없습니다.",
                    chatId,
                    exception
            );
        }
    }

    private void validateChatId(Long requestedChatId, Long responseChatId) {
        if (!requestedChatId.equals(responseChatId)) {
            throw new AiChatException(
                    "AI_CHAT_ID_MISMATCH",
                    "요청한 채팅방과 AI 응답의 채팅방이 일치하지 않습니다.",
                    requestedChatId
            );
        }
    }
}
