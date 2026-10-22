package com.ktb.lookddak.global.security.internal;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "internal.api")
public class InternalApiKeyProperties {

    private String key;
}
