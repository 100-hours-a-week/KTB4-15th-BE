package com.ktb.lookddak.domain.fitting.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FittingCandidateBulkDeleteRequest {

    @NotEmpty
    @Size(max = 1_000)
    private List<@NotNull @Positive Long> fittingCandidateIds;

    public FittingCandidateBulkDeleteRequest(
            List<Long> fittingCandidateIds
    ) {
        this.fittingCandidateIds = fittingCandidateIds;
    }
}
