package com.ktb.lookddak.domain.chat.repository;

import com.ktb.lookddak.domain.chat.entity.ChatGenerationStatus;
import com.ktb.lookddak.domain.chat.entity.ChatMessage;
import com.ktb.lookddak.domain.chat.entity.ChatSenderType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    boolean existsByChatRoomIdAndSenderTypeAndGenerationStatus(
            Long chatRoomId,
            ChatSenderType senderType,
            ChatGenerationStatus generationStatus
    );

    boolean existsByIdAndChatRoomId(Long messageId, Long chatRoomId);

    Optional<ChatMessage> findByIdAndChatRoomId(
            Long messageId,
            Long chatRoomId
    );

    Optional<ChatMessage> findFirstByChatRoomIdAndIdGreaterThanOrderByIdAsc(
            Long chatRoomId,
            Long messageId
    );

    @Query("""
            select message
            from ChatMessage message
            where message.chatRoom.id = :chatRoomId
            order by message.id desc
            """)
    List<ChatMessage> findFirstPage(
            @Param("chatRoomId") Long chatRoomId,
            Pageable pageable
    );

    @Query("""
            select message
            from ChatMessage message
            where message.chatRoom.id = :chatRoomId
              and message.id < :cursor
            order by message.id desc
            """)
    List<ChatMessage> findPreviousPage(
            @Param("chatRoomId") Long chatRoomId,
            @Param("cursor") Long cursor,
            Pageable pageable
    );
}
