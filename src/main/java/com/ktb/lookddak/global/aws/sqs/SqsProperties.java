package com.ktb.lookddak.global.aws.sqs;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "aws.sqs")
public class SqsProperties {

    @NotBlank
    private final String region;

    private final String fittingRequestQueueUrl;
    private final String fittingResponseQueueUrl;

    public SqsProperties(
            String region,
            String fittingRequestQueueUrl,
            String fittingResponseQueueUrl
    ) {
        this.region = region;
        this.fittingRequestQueueUrl = fittingRequestQueueUrl;
        this.fittingResponseQueueUrl = fittingResponseQueueUrl;
    }

    public String getRegion() {
        return region;
    }

    public String getFittingRequestQueueUrl() {
        return fittingRequestQueueUrl;
    }

    public String getFittingResponseQueueUrl() {
        return fittingResponseQueueUrl;
    }
}
