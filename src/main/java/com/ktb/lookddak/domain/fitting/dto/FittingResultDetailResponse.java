package com.ktb.lookddak.domain.fitting.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.ktb.lookddak.domain.fitting.entity.FittingResult;
import lombok.Getter;

import java.util.List;

@Getter
@JsonPropertyOrder({
        "fittingResultId",
        "resultImageUrl",
        "outfitName",
        "comment",
        "products"
})
public class FittingResultDetailResponse {

    private final Long fittingResultId;
    private final String resultImageUrl;
    private final String outfitName;
    private final String comment;
    private final List<FittingResultProductResponse> products;

    private FittingResultDetailResponse(
            Long fittingResultId,
            String resultImageUrl,
            String outfitName,
            String comment,
            List<FittingResultProductResponse> products
    ) {
        this.fittingResultId = fittingResultId;
        this.resultImageUrl = resultImageUrl;
        this.outfitName = outfitName;
        this.comment = comment;
        this.products = List.copyOf(products);
    }

    public static FittingResultDetailResponse from(
            FittingResult fittingResult,
            String resultImageUrl,
            List<FittingResultProductResponse> products
    ) {
        return new FittingResultDetailResponse(
                fittingResult.getId(),
                resultImageUrl,
                fittingResult.getOutfitName(),
                fittingResult.getAiComment(),
                products
        );
    }
}
