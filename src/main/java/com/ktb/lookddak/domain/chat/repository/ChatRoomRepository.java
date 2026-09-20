package com.ktb.lookddak.domain.chat.repository;

import com.ktb.lookddak.domain.chat.entity.ChatRoom;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select chatRoom
            from ChatRoom chatRoom
            where chatRoom.id = :chatRoomId
              and chatRoom.deletedAt is null
            """)
    Optional<ChatRoom> findActiveByIdForUpdate(
            @Param("chatRoomId") Long chatRoomId
    );
}
