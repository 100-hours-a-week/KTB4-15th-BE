package com.ktb.lookddak.global.storage.s3;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class S3PresignedUrlProviderTest {

    private final S3Properties properties = new S3Properties(
            "lookddak-test-images",
            "ap-northeast-2",
            Duration.ofMinutes(10)
    );

    @Test
    @DisplayName("S3 Key로 일정 시간 유효한 GET URL을 생성한다")
    void createPresignedGetUrl() {
        try (S3Presigner presigner = createPresigner()) {
            S3PresignedUrlProvider provider =
                    new S3PresignedUrlProvider(presigner, properties);

            String url = provider.createGetUrl(
                    "/users/1/body-images/validated.png"
            );

            assertThat(url)
                    .startsWith(
                            "https://lookddak-test-images.s3.ap-northeast-2.amazonaws.com/"
                    )
                    .contains("users/1/body-images/validated.png")
                    .contains("X-Amz-Algorithm=AWS4-HMAC-SHA256")
                    .contains("X-Amz-Expires=600");
        }
    }

    @Test
    @DisplayName("S3 Key가 비어 있으면 URL을 생성하지 않는다")
    void rejectBlankKey() {
        try (S3Presigner presigner = createPresigner()) {
            S3PresignedUrlProvider provider =
                    new S3PresignedUrlProvider(presigner, properties);

            assertThatThrownBy(() -> provider.createGetUrl(" "))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    private S3Presigner createPresigner() {
        return S3Presigner.builder()
                .region(Region.AP_NORTHEAST_2)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                                "test-access-key",
                                "test-secret-key"
                        )
                ))
                .build();
    }
}
