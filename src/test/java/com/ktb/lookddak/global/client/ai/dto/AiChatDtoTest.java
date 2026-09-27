package com.ktb.lookddak.global.client.ai.dto;

import com.ktb.lookddak.domain.chat.entity.ChatSourceType;
import com.ktb.lookddak.domain.product.entity.ProductItemType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiChatDtoTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    @DisplayName("AI 채팅 요청을 snake_case 계약으로 직렬화한다")
    void serializeAiChatRequest() throws Exception {
        AiChatRequest request = new AiChatRequest(
                123L,
                1L,
                "소개팅용 네이비 셔츠 추천해줘",
                ChatSourceType.GENERAL,
                List.of()
        );

        JsonNode json = objectMapper.readTree(
                objectMapper.writeValueAsString(request)
        );

        assertThat(json.get("chat_id").asLong()).isEqualTo(123L);
        assertThat(json.get("user_id").asLong()).isEqualTo(1L);
        assertThat(json.get("message").asText())
                .isEqualTo("소개팅용 네이비 셔츠 추천해줘");
        assertThat(json.get("source_type").asText()).isEqualTo("GENERAL");
        assertThat(json.get("product_ids").isArray()).isTrue();
        assertThat(json.has("chatId")).isFalse();
    }

    @Test
    @DisplayName("done 이벤트의 최종 문장과 전체 추천 상품을 역직렬화한다")
    void deserializeDoneResponse() throws Exception {
        String response = """
                {
                  "chat_id": 123,
                  "content": "조건에 맞는 상품을 찾아봤어요.",
                  "products": [
                    {
                      "product_code": "0000001",
                      "product_name": "오버핏 코튼 셔츠",
                      "image_url": "https://example.com/products/0000001.jpg",
                      "detail_url": "https://shop.example.com/products/0000001",
                      "color": "NAVY",
                      "item_type": "TOP",
                      "price": 69000,
                      "llm_comment": "소개팅 룩에 잘 어울립니다."
                    }
                  ]
                }
                """;

        AiChatDoneResponse result = objectMapper.readValue(
                response,
                AiChatDoneResponse.class
        );

        assertThat(result.getChatId()).isEqualTo(123L);
        assertThat(result.getContent())
                .isEqualTo("조건에 맞는 상품을 찾아봤어요.");
        assertThat(result.getProducts())
                .singleElement()
                .satisfies(product -> {
                    assertThat(product.getProductCode()).isEqualTo("0000001");
                    assertThat(product.getProductName())
                            .isEqualTo("오버핏 코튼 셔츠");
                    assertThat(product.getItemType())
                            .isEqualTo(ProductItemType.TOP);
                    assertThat(product.getPrice()).isEqualTo(69_000);
                    assertThat(product.getLlmComment())
                            .isEqualTo("소개팅 룩에 잘 어울립니다.");
                });
    }

    @Test
    @DisplayName("상품이 없는 done 이벤트는 빈 상품 목록으로 변환한다")
    void deserializeDoneResponseWithoutProducts() throws Exception {
        AiChatDoneResponse result = objectMapper.readValue(
                """
                        {
                          "chat_id": 123,
                          "content": "소개팅용 네이비 셔츠로 추천해드릴까요?",
                          "products": []
                        }
                        """,
                AiChatDoneResponse.class
        );

        assertThat(result.getProducts()).isEmpty();
    }

    @Test
    @DisplayName("AI error 이벤트를 오류 응답 DTO로 역직렬화한다")
    void deserializeErrorResponse() throws Exception {
        AiChatErrorResponse result = objectMapper.readValue(
                """
                        {
                          "chat_id": 123,
                          "code": "recommendation_search_failed",
                          "message": "상품 추천 처리 중 오류가 발생했습니다."
                        }
                        """,
                AiChatErrorResponse.class
        );

        assertThat(result.getChatId()).isEqualTo(123L);
        assertThat(result.getCode()).isEqualTo("recommendation_search_failed");
        assertThat(result.getMessage())
                .isEqualTo("상품 추천 처리 중 오류가 발생했습니다.");
    }
}
