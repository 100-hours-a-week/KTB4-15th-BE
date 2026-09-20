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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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
        ReflectionTestUtils.setField(
                message,
                "generationStatus",
                ChatGenerationStatus.COMPLETED
        );
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
}
