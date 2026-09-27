package com.ktb.lookddak.domain.fitting.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.ktb.lookddak.domain.fitting.entity.FittingTempResult;
import lombok.Getter;

import java.util.List;

@Getter
@JsonPropertyOrder({
        "resultImageUrl",
        "outfitName",
        "comment",
        "products"
})
public class FittingResultResponse {

    private final String resultImageUrl;
    private final String outfitName;
    private final String comment;
    private final List<FittingResultProductResponse> products;

    private FittingResultResponse(
            String resultImageUrl,
            String outfitName,
            String comment,
            List<FittingResultProductResponse> products
    ) {
        this.resultImageUrl = resultImageUrl;
        this.outfitName = outfitName;
        this.comment = comment;
        this.products = List.copyOf(products);
    }

    public static FittingResultResponse from(
            FittingTempResult result,
            String resultImageUrl,
            List<FittingResultProductResponse> products
    ) {
        return new FittingResultResponse(
                resultImageUrl,
                result.getOutfitName(),
                result.getAiComment(),
                products
        );
    }
}
