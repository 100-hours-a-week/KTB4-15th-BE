package com.ktb.lookddak.domain.auth.dto;

import lombok.Getter;

@Getter
public class LoginTokens {

    private final String accessToken;
    private final String refreshToken;

    public LoginTokens(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
