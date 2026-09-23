package com.ktb.lookddak.domain.chat.dto;

import com.ktb.lookddak.domain.chat.entity.ChatRoom;
import com.ktb.lookddak.domain.chat.entity.ChatSourceType;
import com.ktb.lookddak.domain.member.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChatRoomListResponseTest {

    @Test
    @DisplayName("채팅방 엔티티를 목록 항목 응답으로 변환한다")
    void createItemFromChatRoom() {
        LocalDateTime lastMessageAt = LocalDateTime.of(2026, 9, 8, 13, 20);
        ChatRoom chatRoom = createChatRoom(25L, lastMessageAt);

        ChatRoomListItemResponse item = ChatRoomListItemResponse.from(chatRoom);

        assertThat(item.getChatRoomId()).isEqualTo(25L);
        assertThat(item.getTitle()).isEqualTo("가을 출근용 니트 추천");
        assertThat(item.getLastMessageAt()).isEqualTo(lastMessageAt);
    }

    @Test
    @DisplayName("다음 페이지가 있으면 마지막 채팅방 ID를 Cursor로 반환한다")
    void createResponseWithNextPage() {
        ChatRoomListItemResponse item = ChatRoomListItemResponse.from(
                createChatRoom(18L, LocalDateTime.of(2026, 9, 7, 21, 10))
        );

        ChatRoomListResponse response = new ChatRoomListResponse(
                List.of(item),
                18L,
                true
        );

        assertThat(response.getItems()).containsExactly(item);
        assertThat(response.getNextCursor()).isEqualTo(18L);
        assertThat(response.isHasNext()).isTrue();
    }

    @Test
    @DisplayName("조회 결과가 없으면 빈 목록과 null Cursor를 반환한다")
    void createEmptyResponse() {
        ChatRoomListResponse response = new ChatRoomListResponse(
                List.of(),
                null,
                false
        );

        assertThat(response.getItems()).isEmpty();
        assertThat(response.getNextCursor()).isNull();
        assertThat(response.isHasNext()).isFalse();
    }

    @Test
    @DisplayName("응답 목록은 생성 후 외부에서 변경할 수 없다")
    void copyItems() {
        ChatRoomListItemResponse item = ChatRoomListItemResponse.from(
                createChatRoom(18L, LocalDateTime.now())
        );
        List<ChatRoomListItemResponse> items = new ArrayList<>();
        items.add(item);

        ChatRoomListResponse response = new ChatRoomListResponse(
                items,
                null,
                false
        );
        items.clear();

        assertThat(response.getItems()).containsExactly(item);
    }

    private ChatRoom createChatRoom(Long chatRoomId, LocalDateTime lastMessageAt) {
        Member member = Member.create("member@lookddak.com", "encoded-password");
        ChatRoom chatRoom = ChatRoom.create(
                member,
                "가을 출근용 니트 추천",
                ChatSourceType.GENERAL,
                lastMessageAt
        );
        ReflectionTestUtils.setField(chatRoom, "id", chatRoomId);
        return chatRoom;
    }
}
