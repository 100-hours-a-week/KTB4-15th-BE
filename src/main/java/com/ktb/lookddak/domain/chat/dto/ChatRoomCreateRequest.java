package com.ktb.lookddak.domain.chat.dto;

import com.ktb.lookddak.domain.chat.entity.ChatSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomCreateRequest {

    @NotBlank
    @Size(max = 500)
    private String content;

    @NotNull
    private ChatSourceType sourceType;

    public ChatRoomCreateRequest(String content, ChatSourceType sourceType) {
        this.content = content;
        this.sourceType = sourceType;
    }
}
