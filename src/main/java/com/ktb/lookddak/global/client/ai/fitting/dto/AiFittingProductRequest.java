package com.ktb.lookddak.global.client.ai.fitting.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class AiFittingProductRequest {

    @JsonProperty("product_code")
    private final String productCode;

    public AiFittingProductRequest(String productCode) {
        this.productCode = productCode;
    }
}
