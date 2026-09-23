package com.ktb.lookddak.global.security.cookie;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "auth.cookie")
public class AuthCookieProperties {

    private final boolean secure;

    @NotBlank
    private final String sameSite;

    public AuthCookieProperties(boolean secure, String sameSite) {
        this.secure = secure;
        this.sameSite = sameSite;
    }

    public boolean isSecure() {
        return secure;
    }

    public String getSameSite() {
        return sameSite;
    }
}
