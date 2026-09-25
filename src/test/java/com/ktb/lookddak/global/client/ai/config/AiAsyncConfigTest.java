package com.ktb.lookddak.global.client.ai.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;

class AiAsyncConfigTest {

    @Test
    @DisplayName("AI 작업 전용 스레드 풀을 설정값으로 생성한다")
    void createAiTaskExecutor() {
        AiTaskProperties properties = new AiTaskProperties(
                2,
                4,
                20,
                Duration.ofSeconds(5)
        );
        AiAsyncConfig config = new AiAsyncConfig();

        Executor executor = config.aiTaskExecutor(properties);

        assertThat(executor).isInstanceOfSatisfying(
                ThreadPoolTaskExecutor.class,
                taskExecutor -> {
                    assertThat(taskExecutor.getCorePoolSize()).isEqualTo(2);
                    assertThat(taskExecutor.getMaxPoolSize()).isEqualTo(4);
                    assertThat(taskExecutor.getThreadNamePrefix())
                            .isEqualTo("ai-chat-");
                    taskExecutor.shutdown();
                }
        );
    }
}
