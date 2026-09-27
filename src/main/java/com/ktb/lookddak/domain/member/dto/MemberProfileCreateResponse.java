package com.ktb.lookddak.domain.member.dto;

import lombok.Getter;

@Getter
public class MemberProfileCreateResponse {

    private final Long profileId;

    public MemberProfileCreateResponse(Long profileId) {
        this.profileId = profileId;
    }
}
