package com.ktb.lookddak.domain.fitting.mq.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class FittingRequestProductMessage {

    private final String productCode;

    @JsonCreator
    public FittingRequestProductMessage(
            @JsonProperty("product_code") String productCode
    ) {
        this.productCode = productCode;
    }
}
