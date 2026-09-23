package com.ktb.lookddak.domain.member.dto;

import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.member.entity.MemberProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MemberProfileGetResponseTest {

    @Test
    @DisplayName("회원과 프로필 정보를 기본정보 조회 응답으로 변환한다")
    void createResponseFromMemberProfile() {
        MemberProfile profile = createProfile();

        MemberProfileGetResponse response =
                MemberProfileGetResponse.from(profile);

        assertThat(response.getEmail()).isEqualTo("test@lookddak.com");
        assertThat(response.getName()).isEqualTo("김민준");
        assertThat(response.getAge()).isEqualTo(29);
        assertThat(response.getHeight()).isEqualByComparingTo("175.0");
        assertThat(response.getWeight()).isEqualByComparingTo("70.0");
        assertThat(response.getFullBodyImageKey())
                .isEqualTo("members/1/full-body/profile.jpg");
        assertThat(response.isPriceAlertEnabled()).isTrue();
    }

    @Test
    @DisplayName("회원 기본정보를 API 명세의 필드 순서로 직렬화한다")
    void serializeResponseInSpecifiedOrder() throws Exception {
        MemberProfileGetResponse response =
                MemberProfileGetResponse.from(createProfile());
        ObjectMapper objectMapper = JsonMapper.builder()
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .build();

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).containsSubsequence(
                "\"email\"",
                "\"name\"",
                "\"age\"",
                "\"height\"",
                "\"weight\"",
                "\"fullBodyImageKey\"",
                "\"priceAlertEnabled\""
        );
        assertThat(json).doesNotContain("fullBodyImageUrl");
    }

    private MemberProfile createProfile() {
        Member member = Member.create(
                "test@lookddak.com",
                "encoded-password"
        );
        member.updatePriceAlertEnabled(true);

        return MemberProfile.create(
                member,
                "김민준",
                29,
                new BigDecimal("175.0"),
                new BigDecimal("70.0"),
                "members/1/full-body/profile.jpg"
        );
    }
}
