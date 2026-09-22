package com.ktb.lookddak.domain.chat.controller;

import com.ktb.lookddak.domain.chat.dto.ChatMessageCreateRequest;
import com.ktb.lookddak.domain.chat.dto.ChatMessageCreateResponse;
import com.ktb.lookddak.domain.chat.dto.ChatGenerationStatusResponse;
import com.ktb.lookddak.domain.chat.dto.ChatRoomCreateRequest;
import com.ktb.lookddak.domain.chat.dto.ChatRoomCreateResponse;
import com.ktb.lookddak.domain.chat.dto.ChatRoomDetailResponse;
import com.ktb.lookddak.domain.chat.dto.ChatRoomListResponse;
import com.ktb.lookddak.domain.chat.dto.ChatRoomTitleUpdateRequest;
import com.ktb.lookddak.domain.chat.dto.ChatRoomTitleUpdateResponse;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/chat-rooms")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping
    public ResponseEntity<ApiResponse<ChatRoomListResponse>> getChatRooms(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") Integer size
    ) {
        ChatRoomListResponse response = chatService.getChatRooms(
                principal.getMemberId(),
                cursor,
                size
        );

        return ResponseEntity
                .status(SuccessCode.OK.getStatus())
                .body(ApiResponse.success(SuccessCode.OK, response));
    }

    @GetMapping("/{chatRoomId}")
    public ResponseEntity<ApiResponse<ChatRoomDetailResponse>> getChatRoomDetail(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Positive @PathVariable Long chatRoomId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") Integer size
    ) {
        ChatRoomDetailResponse response = chatService.getChatRoomDetail(
                principal.getMemberId(),
                chatRoomId,
                cursor,
                size
        );

        return ResponseEntity
                .status(SuccessCode.OK.getStatus())
                .body(ApiResponse.success(SuccessCode.OK, response));
    }

    @GetMapping("/{chatRoomId}/messages/{messageId}/status")
    public ResponseEntity<ApiResponse<ChatGenerationStatusResponse>>
            getGenerationStatus(
                    @AuthenticationPrincipal MemberPrincipal principal,
                    @Positive @PathVariable Long chatRoomId,
                    @Positive @PathVariable Long messageId
            ) {
        ChatGenerationStatusResponse response =
                chatService.getGenerationStatus(
                        principal.getMemberId(),
                        chatRoomId,
                        messageId
                );

        return ResponseEntity
                .status(SuccessCode.OK.getStatus())
                .body(ApiResponse.success(SuccessCode.OK, response));
    }

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

    @PatchMapping("/{chatRoomId}")
    public ResponseEntity<ApiResponse<ChatRoomTitleUpdateResponse>> updateTitle(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Positive @PathVariable Long chatRoomId,
            @Valid @RequestBody ChatRoomTitleUpdateRequest request
    ) {
        ChatRoomTitleUpdateResponse response = chatService.updateTitle(
                principal.getMemberId(),
                chatRoomId,
                request
        );

        return ResponseEntity
                .status(SuccessCode.OK.getStatus())
                .body(ApiResponse.success(SuccessCode.OK, response));
    }

    @DeleteMapping("/{chatRoomId}")
    public ResponseEntity<ApiResponse<Void>> deleteChatRoom(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Positive @PathVariable Long chatRoomId
    ) {
        chatService.deleteChatRoom(principal.getMemberId(), chatRoomId);

        return ResponseEntity
                .status(SuccessCode.OK.getStatus())
                .body(ApiResponse.success(SuccessCode.OK, null));
    }
}
