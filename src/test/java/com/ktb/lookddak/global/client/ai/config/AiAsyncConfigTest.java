package com.ktb.lookddak.global.client.ai.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;

class AiAsyncConfigTest {

    @Test
    @DisplayName("채팅 AI 작업 전용 스레드 풀을 설정값으로 생성한다")
    void createChatTaskExecutor() {
        AiChatTaskProperties properties = new AiChatTaskProperties(
                2,
                4,
                20,
                Duration.ofSeconds(5)
        );
        AiAsyncConfig config = new AiAsyncConfig();

        Executor executor = config.chatTaskExecutor(properties);

        assertExecutor(
                executor,
                2,
                4,
                20,
                "ai-chat-"
        );
    }

    @Test
    @DisplayName("가상피팅 AI 작업 전용 스레드 풀을 설정값으로 생성한다")
    void createFittingTaskExecutor() {
        AiFittingTaskProperties properties = new AiFittingTaskProperties(
                1,
                1,
                5,
                Duration.ofSeconds(120)
        );
        AiAsyncConfig config = new AiAsyncConfig();

        Executor executor = config.fittingTaskExecutor(properties);

        assertExecutor(
                executor,
                1,
                1,
                5,
                "ai-fitting-"
        );
    }

    @Test
    @DisplayName("AI 결과 저장 전용 스레드 풀을 설정값으로 생성한다")
    void createResultPersistenceTaskExecutor() {
        AiResultPersistenceTaskProperties properties =
                new AiResultPersistenceTaskProperties(
                        1,
                        1,
                        50,
                        Duration.ofSeconds(30)
                );
        AiAsyncConfig config = new AiAsyncConfig();

        Executor executor = config.aiResultPersistenceExecutor(properties);

        assertExecutor(
                executor,
                1,
                1,
                50,
                "ai-result-persistence-"
        );
    }

    private void assertExecutor(
            Executor executor,
            int corePoolSize,
            int maxPoolSize,
            int queueCapacity,
            String threadNamePrefix
    ) {
        assertThat(executor).isInstanceOfSatisfying(
                ThreadPoolTaskExecutor.class,
                taskExecutor -> {
                    assertThat(taskExecutor.getCorePoolSize())
                            .isEqualTo(corePoolSize);
                    assertThat(taskExecutor.getMaxPoolSize())
                            .isEqualTo(maxPoolSize);
                    assertThat(taskExecutor.getThreadNamePrefix())
                            .isEqualTo(threadNamePrefix);
                    assertThat(taskExecutor.getThreadPoolExecutor()
                            .getQueue().remainingCapacity())
                            .isEqualTo(queueCapacity);
                    taskExecutor.shutdown();
                }
        );
    }
}
