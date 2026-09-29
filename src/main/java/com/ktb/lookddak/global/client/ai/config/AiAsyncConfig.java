package com.ktb.lookddak.global.client.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;
import java.util.concurrent.Executor;

@EnableAsync
@Configuration
@EnableConfigurationProperties({
        AiChatTaskProperties.class,
        AiFittingTaskProperties.class
})
public class AiAsyncConfig {

    @Bean(name = "chatTaskExecutor")
    public Executor chatTaskExecutor(AiChatTaskProperties properties) {
        return createExecutor(
                properties.getCorePoolSize(),
                properties.getMaxPoolSize(),
                properties.getQueueCapacity(),
                properties.getAwaitTermination(),
                "ai-chat-"
        );
    }

    @Bean(name = "fittingTaskExecutor")
    public Executor fittingTaskExecutor(AiFittingTaskProperties properties) {
        return createExecutor(
                properties.getCorePoolSize(),
                properties.getMaxPoolSize(),
                properties.getQueueCapacity(),
                properties.getAwaitTermination(),
                "ai-fitting-"
        );
    }

    private Executor createExecutor(
            int corePoolSize,
            int maxPoolSize,
            int queueCapacity,
            Duration awaitTermination,
            String threadNamePrefix
    ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationMillis(
                awaitTermination.toMillis()
        );
        executor.initialize();
        return executor;
    }
}
