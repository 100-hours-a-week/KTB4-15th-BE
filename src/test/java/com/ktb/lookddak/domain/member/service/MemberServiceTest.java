package com.ktb.lookddak.domain.member.service;

import com.ktb.lookddak.domain.member.dto.MemberMeResponse;
import com.ktb.lookddak.domain.member.repository.MemberProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberProfileRepository memberProfileRepository;

    @InjectMocks
    private MemberService memberService;

    @Test
    @DisplayName("프로필이 있으면 기본정보 등록 완료를 반환한다")
    void returnCompletedProfile() {
        given(memberProfileRepository.existsByMemberId(1L))
                .willReturn(true);

        MemberMeResponse response = memberService.getMe(1L);

        assertThat(response.isProfileCompleted()).isTrue();
    }

    @Test
    @DisplayName("프로필이 없으면 기본정보 미등록을 반환한다")
    void returnIncompleteProfile() {
        given(memberProfileRepository.existsByMemberId(1L))
                .willReturn(false);

        MemberMeResponse response = memberService.getMe(1L);

        assertThat(response.isProfileCompleted()).isFalse();
    }
}
