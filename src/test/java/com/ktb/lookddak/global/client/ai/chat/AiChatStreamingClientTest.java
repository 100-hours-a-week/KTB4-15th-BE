package com.ktb.lookddak.global.client.ai.chat;

import com.ktb.lookddak.domain.chat.entity.ChatSourceType;
import com.ktb.lookddak.domain.product.entity.ProductItemType;
import com.ktb.lookddak.global.client.ai.config.AiClientProperties;
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

class AiChatStreamingClientTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    @DisplayName("token, products, done 이벤트를 순서대로 스트림 이벤트로 변환한다")
    void streamAllAiEvents() {
        AiChatStreamingClient client = createClient(sseResponse("""
                event: token
                data: {"chat_id":123,"content":"조건에 맞는 상품을 찾아봤어요."}

                event: products
                data: {"chat_id":123,"products":[{"product_code":"0000001","product_name":"오버핏 코튼 셔츠","product_image_url":"https://example.com/1.jpg","purchase_url":"https://shop.example.com/1","color":"NAVY","item_type":"TOP","current_price":69000,"recommended_reason":"소개팅 룩에 잘 어울립니다."},{"product_code":"0000002","product_name":"미니멀 셔츠","product_image_url":"https://example.com/2.jpg","purchase_url":"https://shop.example.com/2","color":"BLUE","item_type":"TOP","current_price":59000,"recommended_reason":"깔끔한 셔츠입니다."}]}

                event: done
                data: {"chat_id":123,"content":"조건에 맞는 상품을 찾아봤어요.","products":[{"product_code":"0000001","product_name":"오버핏 코튼 셔츠","product_image_url":"https://example.com/1.jpg","purchase_url":"https://shop.example.com/1","color":"NAVY","item_type":"TOP","current_price":69000,"recommended_reason":"소개팅 룩에 잘 어울립니다."}]}

                """));

        List<AiChatStreamEvent> events = client.stream(request())
                .collectList()
                .block();

        assertThat(events)
                .extracting(AiChatStreamEvent::getType)
                .containsExactly(
                        AiChatStreamEventType.TOKEN,
                        AiChatStreamEventType.PRODUCTS,
                        AiChatStreamEventType.DONE
                );
        assertThat(events.get(0).getContent())
                .isEqualTo("조건에 맞는 상품을 찾아봤어요.");
        assertThat(events.get(1).getProductsResponse().getProducts())
                .hasSize(2)
                .first()
                .extracting(product -> product.getItemType())
                .isEqualTo(ProductItemType.TOP);
        assertThat(events.get(2).getDoneResponse().getProducts())
                .singleElement()
                .satisfies(product -> assertThat(product.getProductCode()).isEqualTo("0000001"));
    }

    @Test
    @DisplayName("AI가 error 이벤트를 보내면 즉시 AI 채팅 예외를 발생시킨다")
    void throwExceptionOnErrorEvent() {
        AiChatStreamingClient client = createClient(sseResponse("""
                event: error
                data: {"chat_id":123,"code":"SERVER_BUSY","message":"AI 서버가 혼잡합니다."}

                """));

        assertExceptionCode(client, "SERVER_BUSY");
    }

    @Test
    @DisplayName("done 없이 스트림이 닫히면 실패 처리한다")
    void rejectStreamClosedWithoutDone() {
        AiChatStreamingClient client = createClient(sseResponse("""
                event: token
                data: {"chat_id":123,"content":"응답 생성 중"}

                """));

        assertExceptionCode(client, "AI_STREAM_CLOSED");
    }

    @Test
    @DisplayName("이벤트의 chat_id가 요청과 다르면 실패 처리한다")
    void rejectMismatchedChatId() {
        AiChatStreamingClient client = createClient(sseResponse("""
                event: token
                data: {"chat_id":999,"content":"응답 생성 중"}

                """));

        assertExceptionCode(client, "AI_CHAT_ID_MISMATCH");
    }

    @Test
    @DisplayName("이벤트 JSON 형식이 잘못되면 실패 처리한다")
    void rejectInvalidEventJson() {
        AiChatStreamingClient client = createClient(sseResponse("""
                event: token
                data: {invalid-json}

                """));

        assertExceptionCode(client, "AI_INVALID_RESPONSE");
    }

    @Test
    @DisplayName("AI 서버가 HTTP 오류를 반환하면 실패 처리한다")
    void rejectHttpError() {
        AiChatStreamingClient client = createClient(ignored -> Mono.just(
                ClientResponse.create(HttpStatus.TOO_MANY_REQUESTS)
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .body("{\"code\":\"SERVER_BUSY\",\"data\":null,\"message\":\"혼잡합니다.\"}")
                        .build()
        ));

        assertExceptionCode(client, "SERVER_BUSY");
    }

    private void assertExceptionCode(AiChatStreamingClient client, String expectedCode) {
        assertThatThrownBy(() -> client.stream(request()).collectList().block())
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
                        .header("Content-Type", MediaType.TEXT_EVENT_STREAM_VALUE)
                        .body(body)
                        .build()
        );
    }

    private AiChatStreamingClient createClient(ExchangeFunction exchangeFunction) {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(exchangeFunction)
                .build();
        AiClientProperties properties = new AiClientProperties(
                "http://localhost:8000",
                "",
                Duration.ofSeconds(3),
                Duration.ofSeconds(1)
        );
        return new AiChatStreamingClient(webClient, objectMapper, properties);
    }
}
