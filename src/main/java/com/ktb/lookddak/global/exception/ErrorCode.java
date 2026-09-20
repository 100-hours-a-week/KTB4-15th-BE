package com.ktb.lookddak.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common request errors
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    INVALID_PAGINATION_PARAMETER(HttpStatus.BAD_REQUEST, "올바른 조회 조건을 입력해주세요."),
    INVALID_JSON_FORMAT(HttpStatus.BAD_REQUEST, "요청 JSON 형식이 올바르지 않습니다."),
    MISSING_REQUEST_PARAMETER(HttpStatus.BAD_REQUEST, "필수 요청 파라미터가 누락되었습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),

    // Authentication and authorization errors
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 Refresh Token입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    // Member errors
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "중복된 이메일입니다."),

    // Member profile errors
    MEMBER_PROFILE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 기본정보가 등록되어 있습니다."),
    INVALID_FULL_BODY_IMAGE(HttpStatus.BAD_REQUEST, "사용할 수 없는 전신사진 검증 정보입니다."),

    // Chat errors
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."),
    CHAT_ROOM_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 채팅방에 접근할 권한이 없습니다."),
    AI_RESPONSE_GENERATING(HttpStatus.CONFLICT, "AI 응답을 생성 중입니다."),

    // Server errors
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;
}
