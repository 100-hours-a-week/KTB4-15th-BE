package com.ktb.lookddak.domain.member.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberProfileCreateRequest {

    private static final String NAME_PATTERN =
            "^[가-힣A-Za-z]+(?: [가-힣A-Za-z]+)*$";

    @NotBlank
    @Size(max = 10)
    @Pattern(regexp = NAME_PATTERN)
    private String name;

    @NotNull
    @Min(1)
    @Max(100)
    private Integer age;

    @NotNull
    @DecimalMin("100.0")
    @DecimalMax("250.0")
    @Digits(integer = 3, fraction = 1)
    private BigDecimal height;

    @NotNull
    @DecimalMin("30.0")
    @DecimalMax("200.0")
    @Digits(integer = 3, fraction = 1)
    private BigDecimal weight;

    @NotNull
    @Positive
    private Long fullBodyImageValidationId;

    @NotNull
    private Boolean priceAlertEnabled;

    public MemberProfileCreateRequest(
            String name,
            Integer age,
            BigDecimal height,
            BigDecimal weight,
            Long fullBodyImageValidationId,
            Boolean priceAlertEnabled
    ) {
        this.name = name;
        this.age = age;
        this.height = height;
        this.weight = weight;
        this.fullBodyImageValidationId = fullBodyImageValidationId;
        this.priceAlertEnabled = priceAlertEnabled;
    }
}
