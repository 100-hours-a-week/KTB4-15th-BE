package com.ktb.lookddak.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomTitleUpdateRequest {

    @NotBlank
    @Size(max = 20)
    private String title;

    public ChatRoomTitleUpdateRequest(String title) {
        this.title = title;
    }
}
