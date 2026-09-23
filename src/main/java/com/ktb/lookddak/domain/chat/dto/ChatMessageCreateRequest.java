package com.ktb.lookddak.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessageCreateRequest {

    @NotBlank
    @Size(max = 500)
    private String content;

    public ChatMessageCreateRequest(String content) {
        this.content = content;
    }
}
