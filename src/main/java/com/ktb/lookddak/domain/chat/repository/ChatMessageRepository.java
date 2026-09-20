package com.ktb.lookddak.domain.chat.repository;

import com.ktb.lookddak.domain.chat.entity.ChatGenerationStatus;
import com.ktb.lookddak.domain.chat.entity.ChatMessage;
import com.ktb.lookddak.domain.chat.entity.ChatSenderType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    boolean existsByChatRoomIdAndSenderTypeAndGenerationStatus(
            Long chatRoomId,
            ChatSenderType senderType,
            ChatGenerationStatus generationStatus
    );
}
