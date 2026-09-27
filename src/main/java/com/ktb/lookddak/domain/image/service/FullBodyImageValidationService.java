package com.ktb.lookddak.domain.image.service;

import com.ktb.lookddak.domain.image.dto.FullBodyImageValidationResponse;
import com.ktb.lookddak.domain.image.entity.FullBodyImageValidation;
import com.ktb.lookddak.domain.image.repository.FullBodyImageValidationRepository;
import com.ktb.lookddak.domain.image.validation.FullBodyImageFileValidator;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.member.repository.MemberRepository;
import com.ktb.lookddak.global.client.ai.bodyimage.AiBodyImageValidationClient;
import com.ktb.lookddak.global.client.ai.bodyimage.AiBodyImageValidationErrorMapper;
import com.ktb.lookddak.global.client.ai.bodyimage.exception.AiBodyImageValidationException;
import com.ktb.lookddak.global.exception.BusinessException;
import com.ktb.lookddak.global.exception.ErrorCode;
import com.ktb.lookddak.global.storage.s3.S3PresignedUrlProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class FullBodyImageValidationService {

    private final MemberRepository memberRepository;
    private final FullBodyImageValidationRepository validationRepository;
    private final FullBodyImageFileValidator fileValidator;
    private final AiBodyImageValidationClient aiClient;
    private final AiBodyImageValidationErrorMapper errorMapper;
    private final S3PresignedUrlProvider presignedUrlProvider;

    public FullBodyImageValidationResponse validate(
            Long memberId,
            MultipartFile image
    ) {
        fileValidator.validate(image);

        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND
                ));

        String imageKey = requestAiValidation(memberId, image);
        FullBodyImageValidation validation = validationRepository.save(
                FullBodyImageValidation.create(member, imageKey)
        );
        String fullBodyImageUrl = presignedUrlProvider.createGetUrl(imageKey);

        return FullBodyImageValidationResponse.of(
                validation,
                fullBodyImageUrl
        );
    }

    private String requestAiValidation(
            Long memberId,
            MultipartFile image
    ) {
        try {
            return aiClient.validate(memberId, image);
        } catch (AiBodyImageValidationException exception) {
            throw errorMapper.map(exception);
        }
    }
}
