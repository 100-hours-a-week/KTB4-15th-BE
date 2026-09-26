package com.ktb.lookddak.domain.member.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.member.entity.MemberProfile;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@JsonPropertyOrder({
        "email",
        "name",
        "age",
        "height",
        "weight",
        "fullBodyImageUrl",
        "priceAlertEnabled"
})
public class MemberProfileGetResponse {

    private final String email;
    private final String name;
    private final Integer age;
    private final BigDecimal height;
    private final BigDecimal weight;
    private final String fullBodyImageUrl;
    private final boolean priceAlertEnabled;

    private MemberProfileGetResponse(
            String email,
            String name,
            Integer age,
            BigDecimal height,
            BigDecimal weight,
            String fullBodyImageUrl,
            boolean priceAlertEnabled
    ) {
        this.email = email;
        this.name = name;
        this.age = age;
        this.height = height;
        this.weight = weight;
        this.fullBodyImageUrl = fullBodyImageUrl;
        this.priceAlertEnabled = priceAlertEnabled;
    }

    public static MemberProfileGetResponse from(
            MemberProfile profile,
            String fullBodyImageUrl
    ) {
        Member member = profile.getMember();

        return new MemberProfileGetResponse(
                member.getEmail(),
                profile.getName(),
                profile.getAge(),
                profile.getHeight(),
                profile.getWeight(),
                fullBodyImageUrl,
                member.isPriceAlertEnabled()
        );
    }
}
