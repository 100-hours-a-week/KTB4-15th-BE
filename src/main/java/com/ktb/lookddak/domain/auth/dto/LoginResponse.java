package com.ktb.lookddak.domain.auth.dto;

import lombok.Getter;

@Getter
public class LoginResponse {

    private final boolean profileCompleted;

    public LoginResponse(boolean profileCompleted) {
        this.profileCompleted = profileCompleted;
    }
}
