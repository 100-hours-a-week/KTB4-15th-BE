package com.ktb.lookddak.domain.notification.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationReadRequest {

    @NotNull
    @AssertTrue
    private Boolean isRead;

    public NotificationReadRequest(Boolean isRead) {
        this.isRead = isRead;
    }
}
