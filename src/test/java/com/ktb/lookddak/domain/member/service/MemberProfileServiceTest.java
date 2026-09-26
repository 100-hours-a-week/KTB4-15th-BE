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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemberProfileServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberProfileRepository memberProfileRepository;

    @Mock
    private FullBodyImageValidationRepository validationRepository;

    @Mock
    private S3PresignedUrlProvider presignedUrlProvider;

    private MemberProfileService memberProfileService;

    @BeforeEach
    void setUp() {
        memberProfileService = new MemberProfileService(
                memberRepository,
                memberProfileRepository,
                validationRepository,
                presignedUrlProvider
        );
    }

    @Test
    @DisplayName("검증된 전신사진으로 프로필을 생성하고 가격 알림을 설정한다")
    void createProfile() {
        Member member = createMember(1L, "member@lookddak.com");
        FullBodyImageValidation validation = FullBodyImageValidation.create(
                member,
                "full-body/validation/1/test.png"
        );
        MemberProfileCreateRequest request = createRequest(15L);

        given(memberRepository.findByIdAndDeletedAtIsNull(1L))
                .willReturn(Optional.of(member));
        given(memberProfileRepository.existsByMemberId(1L)).willReturn(false);
        given(validationRepository.findById(15L)).willReturn(Optional.of(validation));
        given(memberProfileRepository.save(any(MemberProfile.class)))
                .willAnswer(invocation -> {
                    MemberProfile profile = invocation.getArgument(0);
                    ReflectionTestUtils.setField(profile, "id", 10L);
                    return profile;
                });

        MemberProfileCreateResponse response =
                memberProfileService.createProfile(1L, request);

        ArgumentCaptor<MemberProfile> profileCaptor =
                ArgumentCaptor.forClass(MemberProfile.class);
        verify(memberProfileRepository).save(profileCaptor.capture());

        MemberProfile savedProfile = profileCaptor.getValue();
        assertThat(savedProfile.getMember()).isSameAs(member);
        assertThat(savedProfile.getName()).isEqualTo("John Doe");
        assertThat(savedProfile.getHeight()).isEqualByComparingTo("175.5");
        assertThat(savedProfile.getWeight()).isEqualByComparingTo("70.3");
        assertThat(savedProfile.getFullBodyImageKey())
                .isEqualTo("full-body/validation/1/test.png");
        assertThat(member.isPriceAlertEnabled()).isTrue();
        verify(validationRepository).delete(validation);
        assertThat(response.getProfileId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("이미 프로필이 존재하면 생성을 거부한다")
    void rejectDuplicatedProfile() {
        Member member = createMember(1L, "member@lookddak.com");
        given(memberRepository.findByIdAndDeletedAtIsNull(1L))
                .willReturn(Optional.of(member));
        given(memberProfileRepository.existsByMemberId(1L)).willReturn(true);

        assertThatThrownBy(() ->
                memberProfileService.createProfile(1L, createRequest(15L))
        ).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.MEMBER_PROFILE_ALREADY_EXISTS)
        );

        verify(validationRepository, never()).findById(any());
        verify(memberProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("전신사진 검증 정보가 없으면 프로필 생성을 거부한다")
    void rejectMissingValidation() {
        Member member = createMember(1L, "member@lookddak.com");
        given(memberRepository.findByIdAndDeletedAtIsNull(1L))
                .willReturn(Optional.of(member));
        given(memberProfileRepository.existsByMemberId(1L)).willReturn(false);
        given(validationRepository.findById(15L)).willReturn(Optional.empty());

        assertInvalidFullBodyImage();
    }

    @Test
    @DisplayName("다른 회원의 전신사진 검증 정보로 프로필을 생성할 수 없다")
    void rejectOtherMembersValidation() {
        Member member = createMember(1L, "member@lookddak.com");
        Member otherMember = createMember(2L, "other@lookddak.com");
        FullBodyImageValidation validation = FullBodyImageValidation.create(
                otherMember,
                "full-body/validation/2/test.png"
        );

        given(memberRepository.findByIdAndDeletedAtIsNull(1L))
                .willReturn(Optional.of(member));
        given(memberProfileRepository.existsByMemberId(1L)).willReturn(false);
        given(validationRepository.findById(15L)).willReturn(Optional.of(validation));

        assertInvalidFullBodyImage();
    }

    @Test
    @DisplayName("회원 기본정보와 가격 알림 설정을 조회한다")
    void getProfile() {
        Member member = createMember(1L, "member@lookddak.com");
        member.updatePriceAlertEnabled(true);
        MemberProfile profile = createProfile(member);
        given(memberProfileRepository.findActiveByMemberIdWithMember(1L))
                .willReturn(Optional.of(profile));
        given(presignedUrlProvider.createGetUrl(
                "full-body/validation/1/test.png"
        )).willReturn("https://presigned.example.com/full-body.png");

        MemberProfileGetResponse response = memberProfileService.getProfile(1L);

        assertThat(response.getEmail()).isEqualTo("member@lookddak.com");
        assertThat(response.getName()).isEqualTo("John Doe");
        assertThat(response.getAge()).isEqualTo(29);
        assertThat(response.getHeight()).isEqualByComparingTo("175.5");
        assertThat(response.getWeight()).isEqualByComparingTo("70.3");
        assertThat(response.getFullBodyImageUrl())
                .isEqualTo("https://presigned.example.com/full-body.png");
        assertThat(response.isPriceAlertEnabled()).isTrue();
        verify(presignedUrlProvider).createGetUrl(
                "full-body/validation/1/test.png"
        );
    }

    @Test
    @DisplayName("회원 기본정보가 없으면 조회를 거부한다")
    void rejectMissingProfile() {
        given(memberProfileRepository.findActiveByMemberIdWithMember(1L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> memberProfileService.getProfile(1L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(
                                ErrorCode.MEMBER_PROFILE_NOT_FOUND
                        )
                );
    }

    private void assertInvalidFullBodyImage() {
        assertThatThrownBy(() ->
                memberProfileService.createProfile(1L, createRequest(15L))
        ).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_FULL_BODY_IMAGE)
        );

        verify(memberProfileRepository, never()).save(any());
        verify(validationRepository, never()).delete(any());
    }

    private Member createMember(Long id, String email) {
        Member member = Member.create(email, "encoded-password");
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private MemberProfileCreateRequest createRequest(Long validationId) {
        return new MemberProfileCreateRequest(
                "John Doe",
                29,
                new BigDecimal("175.5"),
                new BigDecimal("70.3"),
                validationId,
                true
        );
    }

    private MemberProfile createProfile(Member member) {
        return MemberProfile.create(
                member,
                "John Doe",
                29,
                new BigDecimal("175.5"),
                new BigDecimal("70.3"),
                "full-body/validation/1/test.png"
        );
    }
}
