package com.ktb.lookddak.domain.product.exception;

public class ProductPriceSyncInvalidRequestException extends RuntimeException {

    public ProductPriceSyncInvalidRequestException() {
        super("invalid_price_sync_request");
    }
}
