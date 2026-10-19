package com.ktb.lookddak.domain.fitting.mq.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;

import java.util.List;

@Getter
@JsonPropertyOrder({
        "fitting_job_id", "requested_at", "user_image_key", "products"
})
public class FittingRequestMessage {

    private final Long fittingJobId;
    private final String requestedAt;
    private final String userImageKey;
    private final List<FittingRequestProductMessage> products;

    @JsonCreator
    public FittingRequestMessage(
            @JsonProperty("fitting_job_id") Long fittingJobId,
            @JsonProperty("requested_at") String requestedAt,
            @JsonProperty("user_image_key") String userImageKey,
            @JsonProperty("products") List<FittingRequestProductMessage> products
    ) {
        this.fittingJobId = fittingJobId;
        this.requestedAt = requestedAt;
        this.userImageKey = userImageKey;
        this.products = List.copyOf(products);
    }
}
