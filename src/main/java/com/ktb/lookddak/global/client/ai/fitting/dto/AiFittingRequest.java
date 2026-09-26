package com.ktb.lookddak.global.client.ai.fitting.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;

import java.util.List;

@Getter
@JsonPropertyOrder({"user_image_url", "products"})
public class AiFittingRequest {

    @JsonProperty("user_image_url")
    private final String userImageUrl;

    private final List<AiFittingProductRequest> products;

    public AiFittingRequest(
            String userImageUrl,
            List<AiFittingProductRequest> products
    ) {
        this.userImageUrl = userImageUrl;
        this.products = List.copyOf(products);
    }
}
