package com.ktb.lookddak.domain.chat.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ChatRoomTitleUpdateRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    @DisplayName("20자 이하의 채팅방 제목을 허용한다")
    void acceptValidTitle() {
        ChatRoomTitleUpdateRequest request =
                new ChatRoomTitleUpdateRequest("가을 출근용 니트 추천");

        Set<ConstraintViolation<ChatRoomTitleUpdateRequest>> violations =
                validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("비어 있거나 공백만 있는 채팅방 제목을 거부한다")
    void rejectBlankTitle() {
        ChatRoomTitleUpdateRequest request =
                new ChatRoomTitleUpdateRequest("   ");

        assertThat(invalidFields(request)).contains("title");
    }

    @Test
    @DisplayName("20자를 초과하는 채팅방 제목을 거부한다")
    void rejectTooLongTitle() {
        ChatRoomTitleUpdateRequest request =
                new ChatRoomTitleUpdateRequest("123456789012345678901");

        assertThat(invalidFields(request)).contains("title");
    }

    private Set<String> invalidFields(ChatRoomTitleUpdateRequest request) {
        return validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());
    }
}
