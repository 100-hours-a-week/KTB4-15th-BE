package com.ktb.lookddak.domain.chat.entity;

import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "chat_room")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseEntity {

    private static final int MAX_TITLE_LENGTH = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = MAX_TITLE_LENGTH)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private ChatSourceType sourceType;

    @Column(name = "last_message_at", nullable = false)
    private LocalDateTime lastMessageAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    private ChatRoom(
            Member member,
            String title,
            ChatSourceType sourceType,
            LocalDateTime lastMessageAt
    ) {
        this.member = member;
        this.title = title;
        this.sourceType = sourceType;
        this.lastMessageAt = lastMessageAt;
    }

    public static ChatRoom create(
            Member member,
            String firstMessageContent,
            ChatSourceType sourceType,
            LocalDateTime lastMessageAt
    ) {
        return new ChatRoom(
                member,
                createTitle(firstMessageContent),
                sourceType,
                lastMessageAt
        );
    }

    public boolean isOwnedBy(Long memberId) {
        return member.getId().equals(memberId);
    }

    public void updateLastMessageAt(LocalDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public void updateTitle(String title) {
        this.title = title.trim();
    }

    public void delete(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    private static String createTitle(String content) {
        String trimmedContent = content.trim();

        return trimmedContent.codePoints()
                .limit(MAX_TITLE_LENGTH)
                .collect(
                        StringBuilder::new,
                        StringBuilder::appendCodePoint,
                        StringBuilder::append
                )
                .toString();
    }
}
