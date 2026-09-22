package com.ktb.lookddak.domain.chat.repository;

import com.ktb.lookddak.domain.chat.entity.ChatRoom;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("""
            select chatRoom
            from ChatRoom chatRoom
            where chatRoom.id = :chatRoomId
              and chatRoom.deletedAt is null
            """)
    Optional<ChatRoom> findActiveById(
            @Param("chatRoomId") Long chatRoomId
    );

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

    @Query("""
            select chatRoom
            from ChatRoom chatRoom
            where chatRoom.id = :cursorId
              and chatRoom.member.id = :memberId
              and chatRoom.deletedAt is null
            """)
    Optional<ChatRoom> findActiveCursor(
            @Param("memberId") Long memberId,
            @Param("cursorId") Long cursorId
    );

    @Query("""
            select chatRoom
            from ChatRoom chatRoom
            where chatRoom.member.id = :memberId
              and chatRoom.deletedAt is null
            order by chatRoom.lastMessageAt desc, chatRoom.id desc
            """)
    List<ChatRoom> findFirstPage(
            @Param("memberId") Long memberId,
            Pageable pageable
    );

    @Query("""
            select chatRoom
            from ChatRoom chatRoom
            where chatRoom.member.id = :memberId
              and chatRoom.deletedAt is null
              and (
                  chatRoom.lastMessageAt < :cursorLastMessageAt
                  or (
                      chatRoom.lastMessageAt = :cursorLastMessageAt
                      and chatRoom.id < :cursorId
                  )
              )
            order by chatRoom.lastMessageAt desc, chatRoom.id desc
            """)
    List<ChatRoom> findNextPage(
            @Param("memberId") Long memberId,
            @Param("cursorLastMessageAt") LocalDateTime cursorLastMessageAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );
}
