package com.ktb.lookddak.domain.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "chat_message",
        indexes = @Index(
                name = "idx_chat_message_generation",
                columnList = "chat_room_id,sender_type,generation_status"
        )
)
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false, length = 20)
    private ChatSenderType senderType;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 30)
    private ChatMessageType messageType;

    @Enumerated(EnumType.STRING)
    @Column(name = "generation_status", length = 20)
    private ChatGenerationStatus generationStatus;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private ChatMessage(
            ChatRoom chatRoom,
            ChatSenderType senderType,
            ChatMessageType messageType,
            ChatGenerationStatus generationStatus,
            String content
    ) {
        this.chatRoom = chatRoom;
        this.senderType = senderType;
        this.messageType = messageType;
        this.generationStatus = generationStatus;
        this.content = content;
    }

    public static ChatMessage createUserText(ChatRoom chatRoom, String content) {
        return new ChatMessage(
                chatRoom,
                ChatSenderType.USER,
                ChatMessageType.TEXT,
                ChatGenerationStatus.GENERATING,
                content
        );
    }
}
