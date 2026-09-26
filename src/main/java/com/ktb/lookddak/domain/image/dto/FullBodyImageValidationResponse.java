package com.ktb.lookddak.domain.image.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.ktb.lookddak.domain.image.entity.FullBodyImageValidation;
import lombok.Getter;

@Getter
@JsonPropertyOrder({"validationId", "fullBodyImageUrl"})
public class FullBodyImageValidationResponse {

    private final Long validationId;
    private final String fullBodyImageUrl;

    private FullBodyImageValidationResponse(
            Long validationId,
            String fullBodyImageUrl
    ) {
        this.validationId = validationId;
        this.fullBodyImageUrl = fullBodyImageUrl;
    }

    public static FullBodyImageValidationResponse of(
            FullBodyImageValidation validation,
            String fullBodyImageUrl
    ) {
        return new FullBodyImageValidationResponse(
                validation.getId(),
                fullBodyImageUrl
        );
    }
}
