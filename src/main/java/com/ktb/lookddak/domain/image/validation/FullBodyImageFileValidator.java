package com.ktb.lookddak.domain.image.validation;

import com.ktb.lookddak.global.exception.BusinessException;
import com.ktb.lookddak.global.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Component
public class FullBodyImageFileValidator {

    static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;

    private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png"
    );

    public void validate(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new BusinessException(ErrorCode.IMAGE_EMPTY);
        }
        if (image.getSize() > MAX_IMAGE_BYTES) {
            throw new BusinessException(ErrorCode.IMAGE_TOO_LARGE);
        }
        if (!SUPPORTED_CONTENT_TYPES.contains(image.getContentType())) {
            throw new BusinessException(
                    ErrorCode.IMAGE_FORMAT_UNSUPPORTED
            );
        }
    }
}
