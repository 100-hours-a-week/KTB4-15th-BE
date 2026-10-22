package com.ktb.lookddak.domain.fitting.mq;

import com.ktb.lookddak.domain.fitting.mq.message.FittingResultMessage;
import com.ktb.lookddak.domain.fitting.mq.message.FittingResultStatus;
import com.ktb.lookddak.domain.fitting.service.FittingGenerationResultService;
import com.ktb.lookddak.global.client.ai.fitting.dto.AiFittingResultData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FittingResultMessageHandlerTest {

    @Mock
    private FittingGenerationResultService resultService;

    @Test
    void saveCompletedResult() {
        FittingResultMessageHandler handler = new FittingResultMessageHandler(resultService);
        when(resultService.complete(org.mockito.ArgumentMatchers.eq(10L), any()))
                .thenReturn(Optional.empty());

        handler.handle(new FittingResultMessage(
                10L, FittingResultStatus.COMPLETED,
                "fitting/result.png", "가을 출근 룩", "설명",
                null, null
        ));

        ArgumentCaptor<AiFittingResultData> captor =
                ArgumentCaptor.forClass(AiFittingResultData.class);
        verify(resultService).complete(org.mockito.ArgumentMatchers.eq(10L), captor.capture());
        assertThat(captor.getValue().getResultImageKey())
                .isEqualTo("fitting/result.png");
    }

    @Test
    void rejectCompletedMessageWithoutImageKey() {
        FittingResultMessageHandler handler = new FittingResultMessageHandler(resultService);

        assertThatThrownBy(() -> handler.handle(new FittingResultMessage(
                10L, FittingResultStatus.COMPLETED,
                "", "가을 출근 룩", "설명", null, null
        ))).isInstanceOf(IllegalArgumentException.class);
    }
}
