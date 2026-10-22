package com.ktb.lookddak.domain.member.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MemberProfileCreateRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    @DisplayName("한글·영문·숫자·중간 공백과 소수점 한 자리의 기본정보를 허용한다")
    void acceptValidRequest() {
        MemberProfileCreateRequest request = createRequest(
                "John Do123",
                new BigDecimal("175.5"),
                new BigDecimal("70.3")
        );

        Set<ConstraintViolation<MemberProfileCreateRequest>> violations =
                validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("이름에 연속된 공백이 있으면 거부한다")
    void rejectInvalidName() {
        MemberProfileCreateRequest request = createRequest(
                "John  Doe123",
                new BigDecimal("175.5"),
                new BigDecimal("70.3")
        );

        assertThat(invalidFields(request)).contains("name");
    }

    @Test
    @DisplayName("키와 몸무게가 허용 범위를 벗어나면 거부한다")
    void rejectOutOfRangeBodySize() {
        MemberProfileCreateRequest request = createRequest(
                "김민준",
                new BigDecimal("99.9"),
                new BigDecimal("200.1")
        );

        assertThat(invalidFields(request)).contains("height", "weight");
    }

    @Test
    @DisplayName("키와 몸무게의 소수점이 두 자리 이상이면 거부한다")
    void rejectTooManyFractionDigits() {
        MemberProfileCreateRequest request = createRequest(
                "김민준",
                new BigDecimal("175.55"),
                new BigDecimal("70.33")
        );

        assertThat(invalidFields(request)).contains("height", "weight");
    }

    @Test
    @DisplayName("프로필 선택 정보는 생략할 수 있고, 가격 알림 설정은 필수다")
    void allowOmittedProfileOptionalValuesAndRequirePriceAlertEnabled() {
        MemberProfileCreateRequest nameOnlyRequest = new MemberProfileCreateRequest(
                "김민준", null, null, null, null, false
        );
        assertThat(invalidFields(nameOnlyRequest)).isEmpty();

        MemberProfileCreateRequest request = new MemberProfileCreateRequest(
                "김민준",
                0,
                new BigDecimal("175.5"),
                new BigDecimal("70.3"),
                0L,
                null
        );

        assertThat(invalidFields(request))
                .contains("age", "fullBodyImageValidationId", "priceAlertEnabled");
    }

    private MemberProfileCreateRequest createRequest(
            String name,
            BigDecimal height,
            BigDecimal weight
    ) {
        return new MemberProfileCreateRequest(
                name,
                29,
                height,
                weight,
                15L,
                true
        );
    }

    private Set<String> invalidFields(MemberProfileCreateRequest request) {
        return validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());
    }
}
