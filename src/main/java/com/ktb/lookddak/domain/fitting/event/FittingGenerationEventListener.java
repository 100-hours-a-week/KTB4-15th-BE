package com.ktb.lookddak.domain.fitting.event;

import com.ktb.lookddak.domain.fitting.command.FittingGenerationCommand;
import com.ktb.lookddak.domain.fitting.service.FittingGenerationQueryService;
import com.ktb.lookddak.global.client.ai.fitting.AiFittingClient;
import com.ktb.lookddak.global.client.ai.fitting.dto.AiFittingProductRequest;
import com.ktb.lookddak.global.client.ai.fitting.dto.AiFittingRequest;
import com.ktb.lookddak.global.client.ai.fitting.dto.AiFittingResultData;
import com.ktb.lookddak.global.client.ai.fitting.exception.AiFittingException;
import com.ktb.lookddak.global.storage.s3.S3PresignedUrlProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FittingGenerationEventListener {

    private final FittingGenerationQueryService queryService;
    private final S3PresignedUrlProvider presignedUrlProvider;
    private final AiFittingClient aiFittingClient;
    private final ApplicationEventPublisher eventPublisher;

    @Async("aiTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(FittingGenerationRequestedEvent event) {
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
        } catch (AiFittingException exception) {
            log.warn(
                    "AI fitting generation failed. fittingJobId={}, code={}",
                    event.getFittingJobId(),
                    exception.getCode(),
                    exception
            );
            publishFailure(
                    event.getFittingJobId(),
                    exception.getCode(),
                    exception.getMessage()
            );
        } catch (Exception exception) {
            log.error(
                    "Unexpected AI fitting generation error. fittingJobId={}",
                    event.getFittingJobId(),
                    exception
            );
            publishFailure(
                    event.getFittingJobId(),
                    "AI_UNEXPECTED_ERROR",
                    "가상피팅 생성 중 예상하지 못한 오류가 발생했습니다."
            );
        }
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
