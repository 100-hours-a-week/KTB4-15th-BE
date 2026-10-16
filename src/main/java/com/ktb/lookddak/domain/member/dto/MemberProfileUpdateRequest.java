package com.ktb.lookddak.domain.member.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberProfileUpdateRequest {

    private static final String NAME_PATTERN =
            "^[가-힣A-Za-z0-9]+(?: [가-힣A-Za-z0-9]+)*$";

    @Size(max = 10)
    @Pattern(regexp = NAME_PATTERN)
    private String name;

    @Min(1)
    @Max(100)
    private Integer age;

    @DecimalMin("100.0")
    @DecimalMax("250.0")
    @Digits(integer = 3, fraction = 1)
    private BigDecimal height;

    @DecimalMin("30.0")
    @DecimalMax("200.0")
    @Digits(integer = 3, fraction = 1)
    private BigDecimal weight;

    @Positive
    private Long fullBodyImageValidationId;

    public MemberProfileUpdateRequest(
            String name,
            Integer age,
            BigDecimal height,
            BigDecimal weight,
            Long fullBodyImageValidationId
    ) {
        this.name = name;
        this.age = age;
        this.height = height;
        this.weight = weight;
        this.fullBodyImageValidationId = fullBodyImageValidationId;
    }

    @AssertTrue
    @JsonIgnore
    public boolean isAnyFieldProvided() {
        return name != null
                || age != null
                || height != null
                || weight != null
                || fullBodyImageValidationId != null;
    }
}
