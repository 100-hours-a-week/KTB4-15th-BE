package com.ktb.lookddak.domain.member.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemberProfileCreateResponseTest {

    @Test
    @DisplayName("생성된 프로필 ID를 반환한다")
    void createResponse() {
        MemberProfileCreateResponse response =
                new MemberProfileCreateResponse(1L);

        assertThat(response.getProfileId()).isEqualTo(1L);
    }
}
