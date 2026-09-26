package com.ktb.lookddak.domain.fitting.event;

import com.ktb.lookddak.domain.fitting.service.FittingGenerationResultService;
import com.ktb.lookddak.global.client.ai.fitting.dto.AiFittingResultData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FittingGenerationResultEventHandlerTest {

    @Mock
    private FittingGenerationResultService resultService;

    @InjectMocks
    private FittingGenerationResultEventHandler handler;

    @Test
    @DisplayName("가상피팅 성공 이벤트를 결과 저장 서비스에 전달한다")
    void handleSucceededEvent() {
        AiFittingResultData result = new AiFittingResultData(
                "fittings/result.png",
                "코디명",
                "코디 설명"
        );

        handler.handleSucceeded(new FittingGenerationSucceededEvent(
                100L,
                result
        ));

        verify(resultService).complete(100L, result);
    }

    @Test
    @DisplayName("가상피팅 실패 이벤트를 실패 처리 서비스에 전달한다")
    void handleFailedEvent() {
        handler.handleFailed(new FittingGenerationFailedEvent(
                100L,
                "AI_RESPONSE_TIMEOUT",
                "AI 응답 시간을 초과했습니다."
        ));

        verify(resultService).fail(100L);
    }
}
