package com.ktb.lookddak.domain.auth.dto;

import lombok.Getter;

@Getter
public class LoginResult {

    private final LoginTokens tokens;
    private final boolean profileCompleted;

    public LoginResult(LoginTokens tokens, boolean profileCompleted) {
        this.tokens = tokens;
        this.profileCompleted = profileCompleted;
    }
}
