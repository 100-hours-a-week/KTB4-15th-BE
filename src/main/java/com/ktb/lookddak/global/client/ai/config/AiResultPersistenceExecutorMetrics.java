package com.ktb.lookddak.global.client.ai.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * AI 결과 저장 Executor가 작업을 거절한 횟수를 기록한다.
 */
@Component
public class AiResultPersistenceExecutorMetrics {

    private final Counter rejectedCounter;

    public AiResultPersistenceExecutorMetrics(MeterRegistry meterRegistry) {
        rejectedCounter = Counter.builder(
                        "ai.result.persistence.executor.rejected"
                )
                .description("AI 결과 저장 Executor가 거부한 작업 수")
                .register(meterRegistry);
    }

    public void rejected() {
        rejectedCounter.increment();
    }

}
