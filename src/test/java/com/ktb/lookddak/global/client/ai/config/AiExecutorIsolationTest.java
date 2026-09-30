package com.ktb.lookddak.global.client.ai.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiExecutorIsolationTest {

    @Test
    @DisplayName("채팅 Executor가 포화되어도 가상피팅 Executor는 독립적으로 실행된다")
    void executeFittingTaskWhenChatExecutorIsSaturated()
            throws InterruptedException {
        AiAsyncConfig config = new AiAsyncConfig();
        ThreadPoolTaskExecutor chatExecutor = createChatExecutor(config);
        ThreadPoolTaskExecutor fittingExecutor = createFittingExecutor(config);

        assertIsolation(chatExecutor, fittingExecutor);
    }

    @Test
    @DisplayName("가상피팅 Executor가 포화되어도 채팅 Executor는 독립적으로 실행된다")
    void executeChatTaskWhenFittingExecutorIsSaturated()
            throws InterruptedException {
        AiAsyncConfig config = new AiAsyncConfig();
        ThreadPoolTaskExecutor chatExecutor = createChatExecutor(config);
        ThreadPoolTaskExecutor fittingExecutor = createFittingExecutor(config);

        assertIsolation(fittingExecutor, chatExecutor);
    }

    private void assertIsolation(
            ThreadPoolTaskExecutor saturatedExecutor,
            ThreadPoolTaskExecutor availableExecutor
    ) throws InterruptedException {
        CountDownLatch releaseRunningTask = new CountDownLatch(1);
        CountDownLatch runningTaskStarted = new CountDownLatch(1);
        CountDownLatch independentTaskCompleted = new CountDownLatch(1);

        try {
            saturatedExecutor.execute(() -> {
                runningTaskStarted.countDown();
                await(releaseRunningTask);
            });
            assertThat(runningTaskStarted.await(1, TimeUnit.SECONDS)).isTrue();

            // 실행 스레드가 사용 중인 상태에서 대기열까지 채운다.
            saturatedExecutor.execute(() -> {
            });
            assertThat(saturatedExecutor.getActiveCount()).isEqualTo(1);
            assertThat(saturatedExecutor.getThreadPoolExecutor().getQueue())
                    .hasSize(1);

            availableExecutor.execute(independentTaskCompleted::countDown);

            assertThat(independentTaskCompleted.await(1, TimeUnit.SECONDS))
                    .isTrue();
            assertThatThrownBy(() -> saturatedExecutor.execute(() -> {
            })).isInstanceOf(RejectedExecutionException.class);
        } finally {
            releaseRunningTask.countDown();
            saturatedExecutor.shutdown();
            availableExecutor.shutdown();
        }
    }

    private ThreadPoolTaskExecutor createChatExecutor(AiAsyncConfig config) {
        return (ThreadPoolTaskExecutor) config.chatTaskExecutor(
                new AiChatTaskProperties(
                        1,
                        1,
                        1,
                        Duration.ofSeconds(1)
                )
        );
    }

    private ThreadPoolTaskExecutor createFittingExecutor(
            AiAsyncConfig config
    ) {
        return (ThreadPoolTaskExecutor) config.fittingTaskExecutor(
                new AiFittingTaskProperties(
                        1,
                        1,
                        1,
                        Duration.ofSeconds(1)
                )
        );
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
