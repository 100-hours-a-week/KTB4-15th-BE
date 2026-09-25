package com.ktb.lookddak.global.storage.s3;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Component
@RequiredArgsConstructor
public class S3PresignedUrlProvider {

    private final S3Presigner s3Presigner;
    private final S3Properties properties;

    public String createGetUrl(String imageKey) {
        if (!StringUtils.hasText(imageKey)) {
            throw new IllegalArgumentException("S3 객체 Key가 필요합니다.");
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(normalizeKey(imageKey))
                .build();
        GetObjectPresignRequest presignRequest =
                GetObjectPresignRequest.builder()
                        .signatureDuration(
                                properties.getPresignedUrlExpiration()
                        )
                        .getObjectRequest(getObjectRequest)
                        .build();

        return s3Presigner.presignGetObject(presignRequest)
                .url()
                .toString();
    }

    private String normalizeKey(String imageKey) {
        return imageKey.startsWith("/")
                ? imageKey.substring(1)
                : imageKey;
    }
}
