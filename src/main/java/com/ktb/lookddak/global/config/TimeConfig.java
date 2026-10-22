package com.ktb.lookddak.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TimeConfig {

    public static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

    @Bean
    public Clock clock() {
        return Clock.system(KOREA_ZONE_ID);
    }
}
