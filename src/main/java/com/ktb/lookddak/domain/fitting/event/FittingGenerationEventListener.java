package com.ktb.lookddak.domain.fitting.event;

import com.ktb.lookddak.domain.fitting.command.FittingGenerationCommand;
import com.ktb.lookddak.domain.fitting.service.FittingGenerationQueryService;
import com.ktb.lookddak.global.client.ai.config.AiTaskExecutorMonitor;
import com.ktb.lookddak.global.client.ai.fitting.AiFittingClient;
import com.ktb.lookddak.global.client.ai.exception.AiClientException;
import com.ktb.lookddak.global.client.ai.fitting.dto.AiFittingProductRequest;
import com.ktb.lookddak.global.client.ai.fitting.dto.AiFittingRequest;
import com.ktb.lookddak.global.client.ai.fitting.dto.AiFittingResultData;
import com.ktb.lookddak.global.storage.s3.S3PresignedUrlProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class FittingGenerationEventListener {

    private final FittingGenerationQueryService queryService;
    private final S3PresignedUrlProvider presignedUrlProvider;
    private final AiFittingClient aiFittingClient;
    private final ApplicationEventPublisher eventPublisher;
    private final Executor fittingTaskExecutor;
    private final AiTaskExecutorMonitor executorMonitor;

    public FittingGenerationEventListener(
            FittingGenerationQueryService queryService,
            S3PresignedUrlProvider presignedUrlProvider,
            AiFittingClient aiFittingClient,
            ApplicationEventPublisher eventPublisher,
            @Qualifier("fittingTaskExecutor") Executor fittingTaskExecutor,
            AiTaskExecutorMonitor executorMonitor
    ) {
        this.queryService = queryService;
        this.presignedUrlProvider = presignedUrlProvider;
        this.aiFittingClient = aiFittingClient;
        this.eventPublisher = eventPublisher;
        this.fittingTaskExecutor = fittingTaskExecutor;
        this.executorMonitor = executorMonitor;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(FittingGenerationRequestedEvent event) {
        long submittedAtNanos = System.nanoTime();
        try {
            fittingTaskExecutor.execute(
                    () -> generate(event, submittedAtNanos)
            );
            executorMonitor.logStatus("fitting", fittingTaskExecutor);
        } catch (RejectedExecutionException exception) {
            log.warn(
                    "AI fitting task rejected. fittingJobId={}",
                    event.getFittingJobId()
            );
            executorMonitor.logStatus("fitting", fittingTaskExecutor);
            publishFailure(
                    event.getFittingJobId(),
                    "AI_TASK_REJECTED",
                    "AI 가상피팅 요청이 많아 작업을 시작하지 못했습니다."
            );
        }
    }

    private void generate(
            FittingGenerationRequestedEvent event,
            long submittedAtNanos
    ) {
        long startedAtNanos = System.nanoTime();
        log.info(
                "AI_FITTING_STARTED fittingJobId={} queueWaitMs={}",
                event.getFittingJobId(),
                elapsedMillis(submittedAtNanos, startedAtNanos)
        );
        try {
            FittingGenerationCommand command = queryService.createCommand(
                    event.getFittingJobId()
            );
            AiFittingRequest request = createAiRequest(command);
            AiFittingResultData result = aiFittingClient.requestFitting(
                    request
            );

            eventPublisher.publishEvent(
                    new FittingGenerationSucceededEvent(
                            command.getFittingJobId(),
                            result
                    )
            );
            log.info(
                    "AI_FITTING_COMPLETED fittingJobId={} elapsedMs={} totalElapsedMs={}",
                    event.getFittingJobId(),
                    elapsedMillis(startedAtNanos, System.nanoTime()),
                    elapsedMillis(submittedAtNanos, System.nanoTime())
            );
        } catch (AiClientException exception) {
            log.warn(
                    "AI_FITTING_FAILED fittingJobId={} code={} elapsedMs={}",
                    event.getFittingJobId(),
                    exception.getCode(),
                    elapsedMillis(startedAtNanos, System.nanoTime()),
                    exception
            );
            publishFailure(
                    event.getFittingJobId(),
                    exception.getCode(),
                    exception.getMessage()
            );
        } catch (Exception exception) {
            log.error(
                    "AI_FITTING_FAILED fittingJobId={} code={} elapsedMs={}",
                    event.getFittingJobId(),
                    "AI_UNEXPECTED_ERROR",
                    elapsedMillis(startedAtNanos, System.nanoTime()),
                    exception
            );
            publishFailure(
                    event.getFittingJobId(),
                    "AI_UNEXPECTED_ERROR",
                    "가상피팅 생성 중 예상하지 못한 오류가 발생했습니다."
            );
        } finally {
            executorMonitor.logStatus("fitting", fittingTaskExecutor);
        }
    }

    private long elapsedMillis(long startNanos, long endNanos) {
        return TimeUnit.NANOSECONDS.toMillis(endNanos - startNanos);
    }

    private AiFittingRequest createAiRequest(
            FittingGenerationCommand command
    ) {
        String userImageUrl = presignedUrlProvider.createGetUrl(
                command.getFullBodyImageKey()
        );
        List<AiFittingProductRequest> products = new ArrayList<>();
        for (String productCode : command.getProductCodes()) {
            products.add(new AiFittingProductRequest(productCode));
        }
        return new AiFittingRequest(userImageUrl, products);
    }

    private void publishFailure(
            Long fittingJobId,
            String code,
            String message
    ) {
        eventPublisher.publishEvent(new FittingGenerationFailedEvent(
                fittingJobId,
                code,
                message
        ));
    }
}
