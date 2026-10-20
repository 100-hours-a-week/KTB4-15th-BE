package com.ktb.lookddak.global.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SuccessCode {

    OK(HttpStatus.OK, "요청이 성공적으로 처리되었습니다."),
    CREATED(HttpStatus.CREATED, "리소스가 성공적으로 생성되었습니다."),
    PRODUCT_PRICE_UPDATE_SUCCESS(HttpStatus.OK, "상품 가격 동기화를 완료했습니다."),
    PRODUCT_PRICE_UPDATE_PARTIAL_SUCCESS(HttpStatus.OK, "일부 상품 가격 동기화에 실패했습니다.");

    private final HttpStatus status;
    private final String message;
}
