package com.ktb.lookddak.domain.member.dto;

import com.ktb.lookddak.domain.member.entity.MemberProfile;

import java.math.BigDecimal;

public record MemberProfileUpdateResponse(
        String name,
        Integer age,
        BigDecimal height,
        BigDecimal weight,
        String fullBodyImageUrl
) {

    public static MemberProfileUpdateResponse from(
            MemberProfile profile,
            String fullBodyImageUrl
    ) {
        return new MemberProfileUpdateResponse(
                profile.getName(),
                profile.getAge(),
                profile.getHeight(),
                profile.getWeight(),
                fullBodyImageUrl
        );
    }
}
