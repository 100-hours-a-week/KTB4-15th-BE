package com.ktb.lookddak.domain.image.validation;

import com.ktb.lookddak.global.exception.BusinessException;
import com.ktb.lookddak.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FullBodyImageFileValidatorTest {

    private final FullBodyImageFileValidator validator =
            new FullBodyImageFileValidator();

    @Test
    @DisplayName("10MB 이하의 JPG와 PNG 이미지를 허용한다")
    void acceptSupportedImage() {
        MockMultipartFile jpeg = image("body.jpg", "image/jpeg", 1);
        MockMultipartFile png = image("body.png", "image/png", 1);

        assertThatCode(() -> validator.validate(jpeg))
                .doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(png))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("이미지가 비어 있으면 거부한다")
    void rejectEmptyImage() {
        MockMultipartFile image = image("body.png", "image/png", 0);

        assertErrorCode(image, ErrorCode.IMAGE_EMPTY);
    }

    @Test
    @DisplayName("지원하지 않는 이미지 형식이면 거부한다")
    void rejectUnsupportedFormat() {
        MockMultipartFile image = image("body.webp", "image/webp", 1);

        assertErrorCode(image, ErrorCode.IMAGE_FORMAT_UNSUPPORTED);
    }

    @Test
    @DisplayName("이미지가 10MB를 초과하면 거부한다")
    void rejectOversizedImage() {
        MockMultipartFile image = image(
                "body.png",
                "image/png",
                Math.toIntExact(FullBodyImageFileValidator.MAX_IMAGE_BYTES + 1)
        );

        assertErrorCode(image, ErrorCode.IMAGE_TOO_LARGE);
    }

    private void assertErrorCode(
            MockMultipartFile image,
            ErrorCode expectedErrorCode
    ) {
        assertThatThrownBy(() -> validator.validate(image))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(expectedErrorCode)
                );
    }

    private MockMultipartFile image(
            String filename,
            String contentType,
            int size
    ) {
        return new MockMultipartFile(
                "image",
                filename,
                contentType,
                new byte[size]
        );
    }
}
