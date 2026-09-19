package com.ktb.lookddak.domain.member.dto;

import lombok.Getter;

@Getter
public class MemberProfileCreateResponse {

    private final Long profileId;
    private final String fullBodyImageKey;

    public MemberProfileCreateResponse(Long profileId, String fullBodyImageKey) {
        this.profileId = profileId;
        this.fullBodyImageKey = fullBodyImageKey;
    }
}
