package com.ktb.lookddak.domain.fitting.command;

import lombok.Getter;

import java.util.List;

@Getter
public class FittingGenerationCommand {

    private final Long fittingJobId;
    private final String fullBodyImageKey;
    private final List<String> productCodes;

    public FittingGenerationCommand(
            Long fittingJobId,
            String fullBodyImageKey,
            List<String> productCodes
    ) {
        this.fittingJobId = fittingJobId;
        this.fullBodyImageKey = fullBodyImageKey;
        this.productCodes = List.copyOf(productCodes);
    }
}
