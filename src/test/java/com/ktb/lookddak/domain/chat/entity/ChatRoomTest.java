package com.ktb.lookddak.domain.chat.entity;

import com.ktb.lookddak.domain.member.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ChatRoomTest {

    @Test
    @DisplayName("첫 메시지와 출처 유형으로 채팅방을 생성한다")
    void createChatRoom() {
        Member member = Member.create("member@lookddak.com", "encoded-password");
        LocalDateTime lastMessageAt = LocalDateTime.of(2026, 9, 20, 12, 0);

        ChatRoom chatRoom = ChatRoom.create(
                member,
                " 5만원대 캐주얼 니트 추천해줘 ",
                ChatSourceType.WISHLIST,
                lastMessageAt
        );

        assertThat(chatRoom.getMember()).isSameAs(member);
        assertThat(chatRoom.getTitle()).isEqualTo("5만원대 캐주얼 니트 추천해줘");
        assertThat(chatRoom.getSourceType()).isEqualTo(ChatSourceType.WISHLIST);
        assertThat(chatRoom.getLastMessageAt()).isEqualTo(lastMessageAt);
        assertThat(chatRoom.getDeletedAt()).isNull();
        assertThat(chatRoom.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("채팅방 제목은 첫 메시지의 앞 20자로 생성한다")
    void limitTitleLength() {
        Member member = Member.create("member@lookddak.com", "encoded-password");

        ChatRoom chatRoom = ChatRoom.create(
                member,
                "12345678901234567890추가내용",
                ChatSourceType.GENERAL,
                LocalDateTime.now()
        );

        assertThat(chatRoom.getTitle()).isEqualTo("12345678901234567890");
    }

    @Test
    @DisplayName("채팅방의 소유 회원인지 확인한다")
    void checkOwner() {
        Member member = Member.create("member@lookddak.com", "encoded-password");
        ReflectionTestUtils.setField(member, "id", 1L);
        ChatRoom chatRoom = ChatRoom.create(
                member,
                "새로운 대화",
                ChatSourceType.GENERAL,
                LocalDateTime.now()
        );

        assertThat(chatRoom.isOwnedBy(1L)).isTrue();
        assertThat(chatRoom.isOwnedBy(2L)).isFalse();
    }

    @Test
    @DisplayName("마지막 메시지 생성 시각을 변경한다")
    void updateLastMessageAt() {
        Member member = Member.create("member@lookddak.com", "encoded-password");
        LocalDateTime initialTime = LocalDateTime.of(2026, 9, 20, 12, 0);
        LocalDateTime updatedTime = initialTime.plusMinutes(10);
        ChatRoom chatRoom = ChatRoom.create(
                member,
                "새로운 대화",
                ChatSourceType.GENERAL,
                initialTime
        );

        chatRoom.updateLastMessageAt(updatedTime);

        assertThat(chatRoom.getLastMessageAt()).isEqualTo(updatedTime);
    }

    @Test
    @DisplayName("채팅방 제목의 양 끝 공백을 제거해 변경한다")
    void updateTitle() {
        Member member = Member.create("member@lookddak.com", "encoded-password");
        ChatRoom chatRoom = ChatRoom.create(
                member,
                "기존 제목",
                ChatSourceType.GENERAL,
                LocalDateTime.now()
        );

        chatRoom.updateTitle("  가을 출근용 니트 추천  ");

        assertThat(chatRoom.getTitle()).isEqualTo("가을 출근용 니트 추천");
    }

    @Test
    @DisplayName("채팅방을 삭제하면 삭제 시각을 기록한다")
    void deleteChatRoom() {
        Member member = Member.create("member@lookddak.com", "encoded-password");
        ChatRoom chatRoom = ChatRoom.create(
                member,
                "새로운 대화",
                ChatSourceType.GENERAL,
                LocalDateTime.now()
        );
        LocalDateTime deletedAt = LocalDateTime.of(2026, 9, 20, 18, 30);

        chatRoom.delete(deletedAt);

        assertThat(chatRoom.getDeletedAt()).isEqualTo(deletedAt);
        assertThat(chatRoom.isDeleted()).isTrue();
    }
}
