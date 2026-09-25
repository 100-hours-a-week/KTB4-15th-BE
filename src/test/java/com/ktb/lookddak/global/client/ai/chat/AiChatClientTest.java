package com.ktb.lookddak.global.client.ai.chat;

import com.ktb.lookddak.domain.chat.entity.ChatSourceType;
import com.ktb.lookddak.domain.product.entity.ProductItemType;
import com.ktb.lookddak.global.client.ai.config.AiClientProperties;
import com.ktb.lookddak.global.client.ai.dto.AiChatDoneResponse;
import com.ktb.lookddak.global.client.ai.dto.AiChatRequest;
import com.ktb.lookddak.global.client.ai.exception.AiChatException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiChatClientTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    @DisplayName("중간 SSE 이벤트를 무시하고 done의 최종 결과만 반환한다")
    void returnDoneResponse() {
        AiChatClient client = createClient(sseResponse("""
                event: token
                data: {"chat_id":123,"content":"조건에 맞는 상품을 찾아봤어요."}

                event: product
                data: {"chat_id":123,"product":{"product_code":"0000001"}}

                event: done
                data: {"chat_id":123,"content":"조건에 맞는 상품을 찾아봤어요.","products":[{"product_code":"0000001","product_name":"오버핏 코튼 셔츠","image_url":"https://example.com/1.jpg","detail_url":"https://shop.example.com/1","color":"NAVY","item_type":"TOP","price":69000,"llm_comment":"소개팅 룩에 잘 어울립니다."}]}

                """), Duration.ofSeconds(1));

        AiChatDoneResponse response = client.requestChat(request());

        assertThat(response.getChatId()).isEqualTo(123L);
        assertThat(response.getContent())
                .isEqualTo("조건에 맞는 상품을 찾아봤어요.");
        assertThat(response.getProducts())
                .singleElement()
                .satisfies(product -> {
                    assertThat(product.getProductCode()).isEqualTo("0000001");
                    assertThat(product.getItemType())
                            .isEqualTo(ProductItemType.TOP);
                    assertThat(product.getPrice()).isEqualTo(69_000);
                    assertThat(product.getLlmComment())
                            .isEqualTo("소개팅 룩에 잘 어울립니다.");
                });
    }

    @Test
    @DisplayName("error 이벤트를 받으면 즉시 AI 채팅 예외를 발생시킨다")
    void throwExceptionOnErrorEvent() {
        AiChatClient client = createClient(sseResponse("""
                event: error
                data: {"chat_id":123,"code":"recommendation_search_failed","message":"상품 추천 처리 중 오류가 발생했습니다."}

                event: done
                data: {"chat_id":123,"content":"","products":[]}

                """), Duration.ofSeconds(1));

        assertThatThrownBy(() -> client.requestChat(request()))
                .isInstanceOfSatisfying(AiChatException.class, exception -> {
                    assertThat(exception.getCode())
                            .isEqualTo("recommendation_search_failed");
                    assertThat(exception.getChatId()).isEqualTo(123L);
                });
    }

    @Test
    @DisplayName("done 없이 SSE 연결이 종료되면 실패 처리한다")
    void rejectStreamClosedWithoutDone() {
        AiChatClient client = createClient(sseResponse("""
                event: token
                data: {"chat_id":123,"content":"응답 생성 중"}

                """), Duration.ofSeconds(1));

        assertExceptionCode(client, "AI_STREAM_CLOSED");
    }

    @Test
    @DisplayName("AI 응답의 chat_id가 요청과 다르면 실패 처리한다")
    void rejectMismatchedChatId() {
        AiChatClient client = createClient(sseResponse("""
                event: done
                data: {"chat_id":999,"content":"완료","products":[]}

                """), Duration.ofSeconds(1));

        assertExceptionCode(client, "AI_CHAT_ID_MISMATCH");
    }

    @Test
    @DisplayName("done 이벤트의 JSON 형식이 잘못되면 실패 처리한다")
    void rejectInvalidDoneJson() {
        AiChatClient client = createClient(sseResponse("""
                event: done
                data: {invalid-json}

                """), Duration.ofSeconds(1));

        assertExceptionCode(client, "AI_INVALID_RESPONSE");
    }

    @Test
    @DisplayName("AI 서버가 HTTP 오류를 반환하면 실패 처리한다")
    void rejectHttpError() {
        ExchangeFunction exchangeFunction = ignored -> Mono.just(
                ClientResponse.create(HttpStatus.UNAUTHORIZED)
                        .header(
                                "Content-Type",
                                MediaType.APPLICATION_JSON_VALUE
                        )
                        .body("{\"code\":401,\"message\":\"unauthorized\",\"data\":null}")
                        .build()
        );
        AiChatClient client = createClient(
                exchangeFunction,
                Duration.ofSeconds(1)
        );

        assertExceptionCode(client, "AI_HTTP_ERROR");
    }

    @Test
    @DisplayName("전체 응답 제한 시간 안에 done이 오지 않으면 실패 처리한다")
    void rejectResponseTimeout() {
        AiChatClient client = createClient(
                ignored -> Mono.never(),
                Duration.ofMillis(10)
        );

        assertExceptionCode(client, "AI_RESPONSE_TIMEOUT");
    }

    private void assertExceptionCode(AiChatClient client, String expectedCode) {
        assertThatThrownBy(() -> client.requestChat(request()))
                .isInstanceOfSatisfying(AiChatException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(expectedCode)
                );
    }

    private AiChatRequest request() {
        return new AiChatRequest(
                123L,
                1L,
                "소개팅용 네이비 셔츠 추천해줘",
                ChatSourceType.GENERAL,
                List.of()
        );
    }

    private ExchangeFunction sseResponse(String body) {
        return ignored -> Mono.just(
                ClientResponse.create(HttpStatus.OK)
                        .header(
                                "Content-Type",
                                MediaType.TEXT_EVENT_STREAM_VALUE
                        )
                        .body(body)
                        .build()
        );
    }

    private AiChatClient createClient(
            ExchangeFunction exchangeFunction,
            Duration responseTimeout
    ) {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(exchangeFunction)
                .build();
        AiClientProperties properties = new AiClientProperties(
                "http://localhost:8000",
                "",
                Duration.ofSeconds(3),
                responseTimeout
        );
        return new AiChatClient(webClient, objectMapper, properties);
    }
}
