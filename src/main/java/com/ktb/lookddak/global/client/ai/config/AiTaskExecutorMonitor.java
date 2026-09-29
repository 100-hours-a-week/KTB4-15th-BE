package com.ktb.lookddak.global.client.ai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Component
public class AiTaskExecutorMonitor {

    public void logStatus(String taskType, Executor executor) {
        if (!(executor instanceof ThreadPoolTaskExecutor taskExecutor)) {
            return;
        }

        ThreadPoolExecutor threadPool =
                taskExecutor.getThreadPoolExecutor();
        log.info(
                "AI_EXECUTOR_STATUS type={} active={} pool={} queued={} completed={}",
                taskType,
                taskExecutor.getActiveCount(),
                taskExecutor.getPoolSize(),
                threadPool.getQueue().size(),
                threadPool.getCompletedTaskCount()
        );
    }
}
