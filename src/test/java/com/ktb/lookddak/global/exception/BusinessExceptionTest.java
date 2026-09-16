package com.ktb.lookddak.global.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessExceptionTest {

    @Test
    @DisplayName("오류 코드의 정보를 비즈니스 예외에 보관한다")
    void createBusinessException() {
        BusinessException exception = new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo("요청한 리소스를 찾을 수 없습니다.");
    }
}
