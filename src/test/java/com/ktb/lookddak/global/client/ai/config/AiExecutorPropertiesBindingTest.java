package com.ktb.lookddak.global.client.ai.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class AiExecutorPropertiesBindingTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(PropertiesTestConfig.class);

    @Test
    @DisplayName("채팅과 가상피팅 Executor 설정을 각각 바인딩한다")
    void bindTaskProperties() {
        contextRunner
                .withPropertyValues(
                        "ai.chat-task.core-pool-size=2",
                        "ai.chat-task.max-pool-size=4",
                        "ai.chat-task.queue-capacity=20",
                        "ai.chat-task.await-termination=60s",
                        "ai.fitting-task.core-pool-size=1",
                        "ai.fitting-task.max-pool-size=1",
                        "ai.fitting-task.queue-capacity=5",
                        "ai.fitting-task.await-termination=120s",
                        "ai.result-persistence-task.core-pool-size=1",
                        "ai.result-persistence-task.max-pool-size=1",
                        "ai.result-persistence-task.queue-capacity=50",
                        "ai.result-persistence-task.await-termination=30s"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    AiChatTaskProperties chatProperties = context.getBean(
                            AiChatTaskProperties.class
                    );
                    assertThat(chatProperties.getCorePoolSize()).isEqualTo(2);
                    assertThat(chatProperties.getMaxPoolSize()).isEqualTo(4);
                    assertThat(chatProperties.getQueueCapacity()).isEqualTo(20);
                    assertThat(chatProperties.getAwaitTermination())
                            .isEqualTo(Duration.ofSeconds(60));

                    AiFittingTaskProperties fittingProperties = context.getBean(
                            AiFittingTaskProperties.class
                    );
                    assertThat(fittingProperties.getCorePoolSize()).isEqualTo(1);
                    assertThat(fittingProperties.getMaxPoolSize()).isEqualTo(1);
                    assertThat(fittingProperties.getQueueCapacity()).isEqualTo(5);
                    assertThat(fittingProperties.getAwaitTermination())
                            .isEqualTo(Duration.ofSeconds(120));

                    AiResultPersistenceTaskProperties resultProperties =
                            context.getBean(
                                    AiResultPersistenceTaskProperties.class
                            );
                    assertThat(resultProperties.getCorePoolSize())
                            .isEqualTo(1);
                    assertThat(resultProperties.getMaxPoolSize())
                            .isEqualTo(1);
                    assertThat(resultProperties.getQueueCapacity())
                            .isEqualTo(50);
                    assertThat(resultProperties.getAwaitTermination())
                            .isEqualTo(Duration.ofSeconds(30));
                });
    }

    @Test
    @DisplayName("최대 스레드 수가 코어 스레드 수보다 작으면 설정을 거절한다")
    void rejectInvalidPoolSize() {
        contextRunner
                .withPropertyValues(
                        "ai.chat-task.core-pool-size=4",
                        "ai.chat-task.max-pool-size=2",
                        "ai.chat-task.queue-capacity=20",
                        "ai.chat-task.await-termination=60s",
                        "ai.fitting-task.core-pool-size=1",
                        "ai.fitting-task.max-pool-size=1",
                        "ai.fitting-task.queue-capacity=5",
                        "ai.fitting-task.await-termination=120s",
                        "ai.result-persistence-task.core-pool-size=1",
                        "ai.result-persistence-task.max-pool-size=1",
                        "ai.result-persistence-task.queue-capacity=50",
                        "ai.result-persistence-task.await-termination=30s"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({
            AiChatTaskProperties.class,
            AiFittingTaskProperties.class,
            AiResultPersistenceTaskProperties.class
    })
    static class PropertiesTestConfig {
    }
}
