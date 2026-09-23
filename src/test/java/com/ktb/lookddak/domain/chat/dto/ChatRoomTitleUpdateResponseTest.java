package com.ktb.lookddak.domain.chat.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatRoomTitleUpdateResponseTest {

    @Test
    @DisplayName("수정한 채팅방 ID와 제목을 반환한다")
    void createResponse() {
        ChatRoomTitleUpdateResponse response =
                new ChatRoomTitleUpdateResponse(123L, "가을 출근용 니트 추천");

        assertThat(response.getChatRoomId()).isEqualTo(123L);
        assertThat(response.getTitle()).isEqualTo("가을 출근용 니트 추천");
    }
}
