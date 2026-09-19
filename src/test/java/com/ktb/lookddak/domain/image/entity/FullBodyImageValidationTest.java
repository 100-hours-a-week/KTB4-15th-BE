package com.ktb.lookddak.domain.image.entity;

import com.ktb.lookddak.domain.member.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class FullBodyImageValidationTest {

    @Test
    @DisplayName("전신사진 검증 결과의 소유 회원을 확인한다")
    void checkOwner() {
        Member member = Member.create("member@lookddak.com", "encoded-password");
        ReflectionTestUtils.setField(member, "id", 1L);
        FullBodyImageValidation validation = FullBodyImageValidation.create(
                member,
                "full-body/validation/1/test.png"
        );

        assertThat(validation.isOwnedBy(1L)).isTrue();
        assertThat(validation.isOwnedBy(2L)).isFalse();
        assertThat(validation.getImageKey())
                .isEqualTo("full-body/validation/1/test.png");
    }
}
