package com.ktb.lookddak.domain.chat.repository;

import com.ktb.lookddak.domain.chat.entity.ChatGenerationStatus;
import com.ktb.lookddak.domain.chat.entity.ChatMessage;
import com.ktb.lookddak.domain.chat.entity.ChatRoom;
import com.ktb.lookddak.domain.chat.entity.ChatSenderType;
import com.ktb.lookddak.domain.chat.entity.ChatSourceType;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ChatRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("채팅방과 사용자 메시지를 저장한다")
    void saveChatRoomAndMessage() {
        Member member = saveMember("chat@lookddak.com");
        ChatRoom chatRoom = chatRoomRepository.saveAndFlush(
                createChatRoom(member, ChatSourceType.WISHLIST)
        );

        ChatMessage message = chatMessageRepository.saveAndFlush(
                ChatMessage.createUserText(chatRoom, "5만원대 캐주얼 니트 추천해줘")
        );

        assertThat(chatRoom.getId()).isNotNull();
        assertThat(chatRoom.getCreatedAt()).isNotNull();
        assertThat(chatRoom.getUpdatedAt()).isNotNull();
        assertThat(message.getId()).isNotNull();
        assertThat(message.getCreatedAt()).isNotNull();
        assertThat(message.getChatRoom()).isSameAs(chatRoom);

        String sourceType = jdbcTemplate.queryForObject(
                "select source_type from chat_room where id = ?",
                String.class,
                chatRoom.getId()
        );
        String senderType = jdbcTemplate.queryForObject(
                "select sender_type from chat_message where id = ?",
                String.class,
                message.getId()
        );
        String messageType = jdbcTemplate.queryForObject(
                "select message_type from chat_message where id = ?",
                String.class,
                message.getId()
        );
        String generationStatus = jdbcTemplate.queryForObject(
                "select generation_status from chat_message where id = ?",
                String.class,
                message.getId()
        );

        assertThat(sourceType).isEqualTo("WISHLIST");
        assertThat(senderType).isEqualTo("USER");
        assertThat(messageType).isEqualTo("TEXT");
        assertThat(generationStatus).isEqualTo("GENERATING");
    }

    @Test
    @DisplayName("채팅방에 AI 응답 생성 중인 사용자 메시지가 있는지 확인한다")
    void existsGeneratingUserMessage() {
        Member member = saveMember("generating@lookddak.com");
        ChatRoom chatRoom = chatRoomRepository.saveAndFlush(
                createChatRoom(member, ChatSourceType.GENERAL)
        );
        chatMessageRepository.saveAndFlush(
                ChatMessage.createUserText(chatRoom, "옷을 추천해줘")
        );

        boolean exists = chatMessageRepository
                .existsByChatRoomIdAndSenderTypeAndGenerationStatus(
                        chatRoom.getId(),
                        ChatSenderType.USER,
                        ChatGenerationStatus.GENERATING
                );

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("완료된 사용자 메시지는 AI 응답 생성 중으로 판단하지 않는다")
    void doesNotFindCompletedUserMessage() {
        Member member = saveMember("completed@lookddak.com");
        ChatRoom chatRoom = chatRoomRepository.saveAndFlush(
                createChatRoom(member, ChatSourceType.GENERAL)
        );
        ChatMessage message = ChatMessage.createUserText(chatRoom, "옷을 추천해줘");
        message.completeGeneration();
        chatMessageRepository.saveAndFlush(message);

        boolean exists = chatMessageRepository
                .existsByChatRoomIdAndSenderTypeAndGenerationStatus(
                        chatRoom.getId(),
                        ChatSenderType.USER,
                        ChatGenerationStatus.GENERATING
                );

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("메시지 전송을 위해 활성 채팅방을 쓰기 락으로 조회한다")
    void findActiveChatRoomForUpdate() {
        Member member = saveMember("lock@lookddak.com");
        ChatRoom savedChatRoom = chatRoomRepository.saveAndFlush(
                createChatRoom(member, ChatSourceType.GENERAL)
        );

        ChatRoom foundChatRoom = chatRoomRepository
                .findActiveByIdForUpdate(savedChatRoom.getId())
                .orElseThrow();

        assertThat(foundChatRoom).isSameAs(savedChatRoom);
    }

    @Test
    @DisplayName("삭제된 채팅방은 활성 채팅방으로 조회하지 않는다")
    void doesNotFindDeletedChatRoomForUpdate() {
        Member member = saveMember("deleted-room@lookddak.com");
        ChatRoom savedChatRoom = chatRoomRepository.saveAndFlush(
                createChatRoom(member, ChatSourceType.GENERAL)
        );
        savedChatRoom.delete(LocalDateTime.now());
        chatRoomRepository.flush();

        assertThat(chatRoomRepository
                .findActiveByIdForUpdate(savedChatRoom.getId()))
                .isEmpty();
        assertThat(chatRoomRepository.findById(savedChatRoom.getId()))
                .isPresent();
    }

    @Test
    @DisplayName("조회 시 삭제되지 않은 채팅방만 락 없이 조회한다")
    void findActiveChatRoom() {
        Member member = saveMember("detail-room@lookddak.com");
        ChatRoom activeRoom = chatRoomRepository.saveAndFlush(
                createChatRoom(member, ChatSourceType.GENERAL)
        );
        ChatRoom deletedRoom = chatRoomRepository.saveAndFlush(
                createChatRoom(member, ChatSourceType.GENERAL)
        );
        deletedRoom.delete(LocalDateTime.now());
        chatRoomRepository.flush();

        assertThat(chatRoomRepository.findActiveById(activeRoom.getId()))
                .contains(activeRoom);
        assertThat(chatRoomRepository.findActiveById(deletedRoom.getId()))
                .isEmpty();
    }

    @Test
    @DisplayName("채팅방의 최근 메시지를 ID 내림차순으로 제한하여 조회한다")
    void findFirstMessagePage() {
        Member member = saveMember("message-first-page@lookddak.com");
        ChatRoom chatRoom = chatRoomRepository.saveAndFlush(
                createChatRoom(member, ChatSourceType.GENERAL)
        );
        ChatRoom otherRoom = chatRoomRepository.saveAndFlush(
                createChatRoom(member, ChatSourceType.GENERAL)
        );
        ChatMessage oldestMessage = saveMessage(chatRoom, "메시지 1");
        ChatMessage middleMessage = saveMessage(chatRoom, "메시지 2");
        ChatMessage newestMessage = saveMessage(chatRoom, "메시지 3");
        saveMessage(otherRoom, "다른 채팅방 메시지");

        List<ChatMessage> messages = chatMessageRepository.findFirstPage(
                chatRoom.getId(),
                PageRequest.of(0, 2)
        );

        assertThat(messages).containsExactly(newestMessage, middleMessage);
        assertThat(messages).doesNotContain(oldestMessage);
    }

    @Test
    @DisplayName("Cursor보다 ID가 작은 과거 메시지만 내림차순으로 조회한다")
    void findPreviousMessagePage() {
        Member member = saveMember("message-previous-page@lookddak.com");
        ChatRoom chatRoom = chatRoomRepository.saveAndFlush(
                createChatRoom(member, ChatSourceType.GENERAL)
        );
        ChatMessage oldestMessage = saveMessage(chatRoom, "메시지 1");
        ChatMessage previousMessage = saveMessage(chatRoom, "메시지 2");
        ChatMessage cursorMessage = saveMessage(chatRoom, "메시지 3");
        saveMessage(chatRoom, "메시지 4");

        List<ChatMessage> messages = chatMessageRepository.findPreviousPage(
                chatRoom.getId(),
                cursorMessage.getId(),
                PageRequest.of(0, 2)
        );

        assertThat(messages).containsExactly(previousMessage, oldestMessage);
    }

    @Test
    @DisplayName("Cursor 메시지가 현재 채팅방에 속하는지 확인한다")
    void existsCursorMessageInChatRoom() {
        Member member = saveMember("message-cursor@lookddak.com");
        ChatRoom chatRoom = chatRoomRepository.saveAndFlush(
                createChatRoom(member, ChatSourceType.GENERAL)
        );
        ChatRoom otherRoom = chatRoomRepository.saveAndFlush(
                createChatRoom(member, ChatSourceType.GENERAL)
        );
        ChatMessage message = saveMessage(chatRoom, "현재 채팅방 메시지");
        ChatMessage otherMessage = saveMessage(otherRoom, "다른 채팅방 메시지");

        assertThat(chatMessageRepository.existsByIdAndChatRoomId(
                message.getId(),
                chatRoom.getId()
        )).isTrue();
        assertThat(chatMessageRepository.existsByIdAndChatRoomId(
                otherMessage.getId(),
                chatRoom.getId()
        )).isFalse();
    }

    @Test
    @DisplayName("회원의 활성 채팅방을 마지막 메시지 시각과 ID 내림차순으로 조회한다")
    void findFirstPage() {
        Member member = saveMember("list@lookddak.com");
        Member otherMember = saveMember("other-list@lookddak.com");
        LocalDateTime sameTime = LocalDateTime.of(2026, 9, 21, 14, 0);
        ChatRoom olderRoom = saveChatRoom(
                member,
                "오래된 채팅방",
                sameTime.minusHours(1)
        );
        ChatRoom sameTimeLowerIdRoom = saveChatRoom(
                member,
                "같은 시각 낮은 ID",
                sameTime
        );
        ChatRoom sameTimeHigherIdRoom = saveChatRoom(
                member,
                "같은 시각 높은 ID",
                sameTime
        );
        ChatRoom deletedRoom = saveChatRoom(
                member,
                "삭제된 채팅방",
                sameTime.plusHours(1)
        );
        deletedRoom.delete(LocalDateTime.now());
        saveChatRoom(otherMember, "다른 회원 채팅방", sameTime.plusHours(2));
        chatRoomRepository.flush();

        List<ChatRoom> result = chatRoomRepository.findFirstPage(
                member.getId(),
                PageRequest.of(0, 3)
        );

        assertThat(result).containsExactly(
                sameTimeHigherIdRoom,
                sameTimeLowerIdRoom,
                olderRoom
        );
    }

    @Test
    @DisplayName("첫 페이지는 Pageable에서 지정한 개수만 조회한다")
    void limitFirstPage() {
        Member member = saveMember("list-limit@lookddak.com");
        LocalDateTime now = LocalDateTime.now();
        saveChatRoom(member, "채팅방 1", now);
        saveChatRoom(member, "채팅방 2", now.minusMinutes(1));
        saveChatRoom(member, "채팅방 3", now.minusMinutes(2));

        List<ChatRoom> result = chatRoomRepository.findFirstPage(
                member.getId(),
                PageRequest.of(0, 2)
        );

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("현재 회원의 삭제되지 않은 채팅방만 Cursor로 조회한다")
    void findActiveCursor() {
        Member member = saveMember("cursor@lookddak.com");
        Member otherMember = saveMember("other-cursor@lookddak.com");
        ChatRoom activeRoom = saveChatRoom(
                member,
                "활성 채팅방",
                LocalDateTime.now()
        );
        ChatRoom deletedRoom = saveChatRoom(
                member,
                "삭제된 채팅방",
                LocalDateTime.now()
        );
        deletedRoom.delete(LocalDateTime.now());
        ChatRoom otherRoom = saveChatRoom(
                otherMember,
                "다른 회원 채팅방",
                LocalDateTime.now()
        );
        chatRoomRepository.flush();

        assertThat(chatRoomRepository.findActiveCursor(
                member.getId(),
                activeRoom.getId()
        )).contains(activeRoom);
        assertThat(chatRoomRepository.findActiveCursor(
                member.getId(),
                deletedRoom.getId()
        )).isEmpty();
        assertThat(chatRoomRepository.findActiveCursor(
                member.getId(),
                otherRoom.getId()
        )).isEmpty();
    }

    @Test
    @DisplayName("Cursor의 정렬 값보다 뒤에 위치한 채팅방만 조회한다")
    void findNextPage() {
        Member member = saveMember("next-page@lookddak.com");
        LocalDateTime cursorTime = LocalDateTime.of(2026, 9, 21, 14, 0);
        ChatRoom sameTimeLowerIdRoom = saveChatRoom(
                member,
                "같은 시각 낮은 ID",
                cursorTime
        );
        ChatRoom cursorRoom = saveChatRoom(
                member,
                "Cursor 채팅방",
                cursorTime
        );
        ChatRoom olderRoom = saveChatRoom(
                member,
                "오래된 채팅방",
                cursorTime.minusHours(1)
        );
        saveChatRoom(member, "최신 채팅방", cursorTime.plusHours(1));

        List<ChatRoom> result = chatRoomRepository.findNextPage(
                member.getId(),
                cursorRoom.getLastMessageAt(),
                cursorRoom.getId(),
                PageRequest.of(0, 10)
        );

        assertThat(result).containsExactly(sameTimeLowerIdRoom, olderRoom);
    }

    private Member saveMember(String email) {
        return memberRepository.saveAndFlush(
                Member.create(email, "encoded-password")
        );
    }

    private ChatRoom createChatRoom(Member member, ChatSourceType sourceType) {
        return ChatRoom.create(
                member,
                "5만원대 캐주얼 니트 추천해줘",
                sourceType,
                LocalDateTime.now()
        );
    }

    private ChatRoom saveChatRoom(
            Member member,
            String title,
            LocalDateTime lastMessageAt
    ) {
        return chatRoomRepository.saveAndFlush(
                ChatRoom.create(
                        member,
                        title,
                        ChatSourceType.GENERAL,
                        lastMessageAt
                )
        );
    }

    private ChatMessage saveMessage(ChatRoom chatRoom, String content) {
        return chatMessageRepository.saveAndFlush(
                ChatMessage.createUserText(chatRoom, content)
        );
    }
}
