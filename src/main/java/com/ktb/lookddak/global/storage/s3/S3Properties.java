package com.ktb.lookddak.global.storage.s3;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "aws.s3")
public class S3Properties {

    @NotBlank
    private final String bucket;

    @NotBlank
    private final String region;

    @NotNull
    private final Duration presignedUrlExpiration;

    public S3Properties(
            String bucket,
            String region,
            Duration presignedUrlExpiration
    ) {
        this.bucket = bucket;
        this.region = region;
        this.presignedUrlExpiration = presignedUrlExpiration;
    }

    public String getBucket() {
        return bucket;
    }

    public String getRegion() {
        return region;
    }

    public Duration getPresignedUrlExpiration() {
        return presignedUrlExpiration;
    }
}
