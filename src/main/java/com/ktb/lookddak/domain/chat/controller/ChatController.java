package com.ktb.lookddak.domain.chat.controller;

import com.ktb.lookddak.domain.chat.dto.ChatMessageCreateRequest;
import com.ktb.lookddak.domain.chat.dto.ChatMessageCreateResponse;
import com.ktb.lookddak.domain.chat.dto.ChatRoomCreateRequest;
import com.ktb.lookddak.domain.chat.dto.ChatRoomCreateResponse;
import com.ktb.lookddak.domain.chat.service.ChatService;
import com.ktb.lookddak.global.response.ApiResponse;
import com.ktb.lookddak.global.response.SuccessCode;
import com.ktb.lookddak.global.security.principal.MemberPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/chat-rooms")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<ApiResponse<ChatRoomCreateResponse>> createChatRoom(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody ChatRoomCreateRequest request
    ) {
        ChatRoomCreateResponse response = chatService.createChatRoom(
                principal.getMemberId(),
                request
        );

        return ResponseEntity
                .status(SuccessCode.CREATED.getStatus())
                .body(ApiResponse.success(SuccessCode.CREATED, response));
    }

    @PostMapping("/{chatRoomId}/messages")
    public ResponseEntity<ApiResponse<ChatMessageCreateResponse>> createMessage(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Positive @PathVariable Long chatRoomId,
            @Valid @RequestBody ChatMessageCreateRequest request
    ) {
        ChatMessageCreateResponse response = chatService.createMessage(
                principal.getMemberId(),
                chatRoomId,
                request
        );

        return ResponseEntity
                .status(SuccessCode.CREATED.getStatus())
                .body(ApiResponse.success(SuccessCode.CREATED, response));
    }
}
