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
import com.ktb.lookddak.global.exception.ExternalApiException;
import com.ktb.lookddak.global.storage.s3.S3PresignedUrlProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class FullBodyImageValidationServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private FullBodyImageValidationRepository validationRepository;
    @Mock
    private FullBodyImageFileValidator fileValidator;
    @Mock
    private AiBodyImageValidationClient aiClient;
    @Mock
    private AiBodyImageValidationErrorMapper errorMapper;
    @Mock
    private S3PresignedUrlProvider presignedUrlProvider;

    private FullBodyImageValidationService service;

    @BeforeEach
    void setUp() {
        service = new FullBodyImageValidationService(
                memberRepository,
                validationRepository,
                fileValidator,
                aiClient,
                errorMapper,
                presignedUrlProvider
        );
    }

    @Test
    @DisplayName("AI 검증 결과를 저장하고 Presigned URL과 검증 ID를 반환한다")
    void validateImage() {
        Long memberId = 1L;
        String imageKey = "full-body/1/validated.png";
        String imageUrl = "https://s3.example.com/validated.png";
        MockMultipartFile image = image();
        Member member = Member.create("member@test.com", "encoded-password");
        ReflectionTestUtils.setField(member, "id", memberId);

        given(memberRepository.findByIdAndDeletedAtIsNull(memberId))
                .willReturn(Optional.of(member));
        given(aiClient.validate(memberId, image)).willReturn(imageKey);
        given(validationRepository.save(any(FullBodyImageValidation.class)))
                .willAnswer(invocation -> {
            FullBodyImageValidation validation = invocation.getArgument(0);
            ReflectionTestUtils.setField(validation, "id", 15L);
            return validation;
        });
        given(presignedUrlProvider.createGetUrl(imageKey))
                .willReturn(imageUrl);

        FullBodyImageValidationResponse response =
                service.validate(memberId, image);

        assertThat(response.getValidationId()).isEqualTo(15L);
        assertThat(response.getFullBodyImageUrl()).isEqualTo(imageUrl);
        verify(fileValidator).validate(image);
        verify(aiClient).validate(memberId, image);
        verify(presignedUrlProvider).createGetUrl(imageKey);
    }

    @Test
    @DisplayName("AI 검증 오류를 변환하고 검증 정보는 저장하지 않는다")
    void mapAiValidationError() {
        Long memberId = 1L;
        MockMultipartFile image = image();
        Member member = Member.create("member@test.com", "encoded-password");
        AiBodyImageValidationException aiException =
                new AiBodyImageValidationException(
                        "AI_SERVER_UNAVAILABLE",
                        "AI server unavailable",
                        null,
                        null,
                        null
                );
        ExternalApiException mappedException = new ExternalApiException(
                ErrorCode.BODY_IMAGE_AI_SERVER_UNAVAILABLE,
                aiException
        );

        given(memberRepository.findByIdAndDeletedAtIsNull(memberId))
                .willReturn(Optional.of(member));
        given(aiClient.validate(memberId, image)).willThrow(aiException);
        given(errorMapper.map(aiException)).willReturn(mappedException);

        assertThatThrownBy(() -> service.validate(memberId, image))
                .isSameAs(mappedException);
        verifyNoInteractions(validationRepository, presignedUrlProvider);
    }

    @Test
    @DisplayName("활성 회원이 없으면 AI를 호출하지 않는다")
    void rejectMissingMemberBeforeAiRequest() {
        Long memberId = 1L;
        MockMultipartFile image = image();
        given(memberRepository.findByIdAndDeletedAtIsNull(memberId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.validate(memberId, image))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND)
                );
        verifyNoInteractions(
                aiClient,
                validationRepository,
                presignedUrlProvider
        );
    }

    private MockMultipartFile image() {
        return new MockMultipartFile(
                "image",
                "body.png",
                "image/png",
                new byte[]{1}
        );
    }
}
