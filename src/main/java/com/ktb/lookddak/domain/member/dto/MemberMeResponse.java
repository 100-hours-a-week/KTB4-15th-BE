package com.ktb.lookddak.domain.member.dto;

import lombok.Getter;

@Getter
public class MemberMeResponse {

    private final boolean profileCompleted;

    public MemberMeResponse(boolean profileCompleted) {
        this.profileCompleted = profileCompleted;
    }
}
