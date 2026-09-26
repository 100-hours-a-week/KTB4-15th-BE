package com.ktb.lookddak.domain.member.service;

import com.ktb.lookddak.domain.image.entity.FullBodyImageValidation;
import com.ktb.lookddak.domain.image.repository.FullBodyImageValidationRepository;
import com.ktb.lookddak.domain.member.dto.MemberProfileCreateRequest;
import com.ktb.lookddak.domain.member.dto.MemberProfileCreateResponse;
import com.ktb.lookddak.domain.member.dto.MemberProfileGetResponse;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.member.entity.MemberProfile;
import com.ktb.lookddak.domain.member.repository.MemberProfileRepository;
import com.ktb.lookddak.domain.member.repository.MemberRepository;
import com.ktb.lookddak.global.exception.BusinessException;
import com.ktb.lookddak.global.exception.ErrorCode;
import com.ktb.lookddak.global.storage.s3.S3PresignedUrlProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberProfileService {

    private final MemberRepository memberRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final FullBodyImageValidationRepository validationRepository;
    private final S3PresignedUrlProvider presignedUrlProvider;

    @Transactional
    public MemberProfileCreateResponse createProfile(
            Long memberId,
            MemberProfileCreateRequest request
    ) {
        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (memberProfileRepository.existsByMemberId(memberId)) {
            throw new BusinessException(ErrorCode.MEMBER_PROFILE_ALREADY_EXISTS);
        }

        FullBodyImageValidation validation = validationRepository
                .findById(request.getFullBodyImageValidationId())
                .filter(result -> result.isOwnedBy(memberId))
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.INVALID_FULL_BODY_IMAGE)
                );

        MemberProfile profile = MemberProfile.create(
                member,
                request.getName(),
                request.getAge(),
                request.getHeight(),
                request.getWeight(),
                validation.getImageKey()
        );

        MemberProfile savedProfile = memberProfileRepository.save(profile);

        member.updatePriceAlertEnabled(request.getPriceAlertEnabled());

        validationRepository.delete(validation);

        return new MemberProfileCreateResponse(savedProfile.getId());
    }

    public MemberProfileGetResponse getProfile(Long memberId) {
        MemberProfile profile = memberProfileRepository
                .findActiveByMemberIdWithMember(memberId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.MEMBER_PROFILE_NOT_FOUND
                ));

        String fullBodyImageUrl = presignedUrlProvider.createGetUrl(
                profile.getFullBodyImageKey()
        );
        return MemberProfileGetResponse.from(profile, fullBodyImageUrl);
    }
}
