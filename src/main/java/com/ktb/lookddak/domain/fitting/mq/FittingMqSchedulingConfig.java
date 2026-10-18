package com.ktb.lookddak.domain.fitting.mq;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class FittingMqSchedulingConfig {

    /**
     * 결과 큐 Long Poll이 Outbox 발행 주기를 막지 않도록 예약 작업을 병렬 실행한다.
     */
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("fitting-mq-scheduler-");
        return scheduler;
    }
}
