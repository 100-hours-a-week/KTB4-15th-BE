package com.ktb.lookddak.domain.fitting.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class FittingJobCreateRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    @DisplayName("상의와 하의 중 하나 또는 둘 다 선택할 수 있다")
    void acceptSelectedProducts() {
        assertThat(validator.validate(
                new FittingJobCreateRequest(1L, null)
        )).isEmpty();
        assertThat(validator.validate(
                new FittingJobCreateRequest(null, 2L)
        )).isEmpty();
        assertThat(validator.validate(
                new FittingJobCreateRequest(1L, 2L)
        )).isEmpty();
    }

    @Test
    @DisplayName("상품 ID는 전달하는 경우 양수여야 한다")
    void rejectNonPositiveProductIds() {
        FittingJobCreateRequest request =
                new FittingJobCreateRequest(0L, -1L);

        Set<String> invalidFields = validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());

        assertThat(invalidFields)
                .containsExactlyInAnyOrder("topProductId", "bottomProductId");
    }

    @Test
    @DisplayName("상품을 선택하지 않은 요청은 Service에서 비즈니스 규칙으로 검증한다")
    void deferEmptySelectionValidationToService() {
        Set<ConstraintViolation<FittingJobCreateRequest>> violations =
                validator.validate(new FittingJobCreateRequest(null, null));

        assertThat(violations).isEmpty();
    }
}
