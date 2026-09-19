package com.ktb.lookddak.domain.member.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemberProfileCreateResponseTest {

    @Test
    @DisplayName("생성된 프로필 ID와 전신사진 이미지 키를 반환한다")
    void createResponse() {
        MemberProfileCreateResponse response = new MemberProfileCreateResponse(
                1L,
                "full-body/validation/1/test.png"
        );

        assertThat(response.getProfileId()).isEqualTo(1L);
        assertThat(response.getFullBodyImageKey())
                .isEqualTo("full-body/validation/1/test.png");
    }
}
