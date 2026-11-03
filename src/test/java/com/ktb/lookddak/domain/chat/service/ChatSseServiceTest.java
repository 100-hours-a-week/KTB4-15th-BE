package com.ktb.lookddak.domain.chat.service;

import com.ktb.lookddak.domain.chat.entity.ChatMessage;
import com.ktb.lookddak.domain.chat.entity.ChatRoom;
import com.ktb.lookddak.domain.chat.entity.ChatSourceType;
import com.ktb.lookddak.domain.chat.entity.ChatGenerationStatus;
import com.ktb.lookddak.domain.chat.dto.ChatGenerationStatusResponse;
import com.ktb.lookddak.domain.chat.dto.ChatMessageDetailResponse;
import com.ktb.lookddak.domain.chat.redis.ChatRedisProperties;
import com.ktb.lookddak.domain.chat.redis.ChatRedisEvent;
import com.ktb.lookddak.domain.chat.redis.ChatRedisSnapshot;
import com.ktb.lookddak.domain.chat.redis.ChatRedisSnapshotRepository;
import com.ktb.lookddak.domain.chat.repository.ChatMessageRepository;
import com.ktb.lookddak.domain.chat.repository.ChatRoomRepository;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.global.exception.BusinessException;
import com.ktb.lookddak.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ChatSseServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatRedisSnapshotRepository chatRedisSnapshotRepository;

    @Mock
    private ChatService chatService;

    @Test
    @DisplayName("채팅방 소유자는 사용자 메시지 SSE 연결을 생성할 수 있다")
    void connect() {
        ChatSseService service = service();
        Member member = createMember(1L);
        ChatRoom chatRoom = createChatRoom(10L, member);
        ChatMessage message = ChatMessage.createPendingUserText(
                chatRoom,
                "가을 니트를 추천해줘"
        );
        ReflectionTestUtils.setField(message, "id", 100L);
        given(chatRoomRepository.findActiveById(10L))
                .willReturn(Optional.of(chatRoom));
        given(chatMessageRepository.findByIdAndChatRoomId(100L, 10L))
                .willReturn(Optional.of(message));

        SseEmitter emitter = service.connect(1L, 10L, 100L);

        assertThat(emitter.getTimeout()).isEqualTo(300_000L);
        assertThat(service.hasConnection(100L)).isTrue();
    }

    @Test
    @DisplayName("다른 회원의 채팅방에는 SSE 연결할 수 없다")
    void rejectWhenChatRoomIsNotOwned() {
        ChatSseService service = service();
        ChatRoom chatRoom = createChatRoom(10L, createMember(2L));
        given(chatRoomRepository.findActiveById(10L))
                .willReturn(Optional.of(chatRoom));

        assertThatThrownBy(() -> service.connect(1L, 10L, 100L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.CHAT_ROOM_ACCESS_DENIED)
                );
    }

    @Test
    @DisplayName("채팅방에 속하지 않는 메시지에는 SSE 연결할 수 없다")
    void rejectWhenMessageDoesNotExist() {
        ChatSseService service = service();
        ChatRoom chatRoom = createChatRoom(10L, createMember(1L));
        given(chatRoomRepository.findActiveById(10L))
                .willReturn(Optional.of(chatRoom));
        given(chatMessageRepository.findByIdAndChatRoomId(100L, 10L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.connect(1L, 10L, 100L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.CHAT_MESSAGE_NOT_FOUND)
                );
        verify(chatMessageRepository).findByIdAndChatRoomId(100L, 10L);
    }

    @Test
    @DisplayName("Snapshot 조회 중 도착한 done event는 버퍼링 후 Snapshot 뒤에 전달되어 연결을 종료한다")
    void bufferRedisEventWhileRecovering() throws Exception {
        ChatSseService service = service(true);
        Member member = createMember(1L);
        ChatRoom chatRoom = createChatRoom(10L, member);
        ChatMessage message = ChatMessage.createPendingUserText(chatRoom, "가을 니트를 추천해줘");
        ReflectionTestUtils.setField(message, "id", 100L);
        given(chatRoomRepository.findActiveById(10L)).willReturn(Optional.of(chatRoom));
        given(chatMessageRepository.findByIdAndChatRoomId(100L, 10L)).willReturn(Optional.of(message));
        given(chatService.getGenerationStatus(1L, 10L, 100L))
                .willReturn(new ChatGenerationStatusResponse(ChatGenerationStatus.GENERATING, null));

        CountDownLatch snapshotReadStarted = new CountDownLatch(1);
        CountDownLatch releaseSnapshotRead = new CountDownLatch(1);
        given(chatRedisSnapshotRepository.findByMessageId(100L)).willAnswer(invocation -> {
            snapshotReadStarted.countDown();
            releaseSnapshotRead.await(1, TimeUnit.SECONDS);
            return Optional.of(new ChatRedisSnapshot(
                    100L,
                    "기존 본문",
                    1L,
                    com.ktb.lookddak.domain.chat.entity.ChatGenerationStatus.GENERATING
            ));
        });

        CompletableFuture<SseEmitter> connection = CompletableFuture.supplyAsync(
                () -> service.connect(1L, 10L, 100L)
        );
        assertThat(snapshotReadStarted.await(1, TimeUnit.SECONDS)).isTrue();

        service.handleRedisEvent(ChatRedisEvent.done(100L, 2L, "최종 본문", List.of()));
        releaseSnapshotRead.countDown();
        connection.get(1, TimeUnit.SECONDS);

        assertThat(service.hasConnection(100L)).isFalse();
    }

    @Test
    @DisplayName("완료·실패 상태 재연결은 Redis Snapshot 대신 DB 상태로 즉시 종료한다")
    void recoverTerminalResultFromDatabaseBeforeRedisSnapshot() {
        ChatSseService service = service(true);
        Member member = createMember(1L);
        ChatRoom chatRoom = createChatRoom(10L, member);
        ChatMessage message = ChatMessage.createPendingUserText(chatRoom, "가을 니트를 추천해줘");
        ReflectionTestUtils.setField(message, "id", 100L);
        given(chatRoomRepository.findActiveById(10L)).willReturn(Optional.of(chatRoom));
        given(chatMessageRepository.findByIdAndChatRoomId(100L, 10L)).willReturn(Optional.of(message));
        given(chatService.getGenerationStatus(1L, 10L, 100L))
                .willReturn(new ChatGenerationStatusResponse(ChatGenerationStatus.FAILED, null));

        SseEmitter emitter = service.connect(1L, 10L, 100L);

        assertThat(emitter.getTimeout()).isEqualTo(300_000L);
        assertThat(service.hasConnection(100L)).isFalse();
        verify(chatRedisSnapshotRepository, never()).findByMessageId(100L);
    }

    @Test
    @DisplayName("Redis Snapshot 조회 오류 시 local fallback SSE 연결로 전환한다")
    void fallBackToLocalConnectionWhenRedisSnapshotReadFails() {
        ChatSseService service = service(true);
        Member member = createMember(1L);
        ChatRoom chatRoom = createChatRoom(10L, member);
        ChatMessage message = ChatMessage.createPendingUserText(chatRoom, "가을 니트를 추천해줘");
        ReflectionTestUtils.setField(message, "id", 100L);
        given(chatRoomRepository.findActiveById(10L)).willReturn(Optional.of(chatRoom));
        given(chatMessageRepository.findByIdAndChatRoomId(100L, 10L)).willReturn(Optional.of(message));
        given(chatService.getGenerationStatus(1L, 10L, 100L))
                .willReturn(new ChatGenerationStatusResponse(ChatGenerationStatus.GENERATING, null));
        given(chatRedisSnapshotRepository.findByMessageId(100L))
                .willThrow(new IllegalStateException("redis unavailable"));

        SseEmitter emitter = service.connect(1L, 10L, 100L);

        assertThat(emitter.getTimeout()).isEqualTo(300_000L);
        assertThat(service.hasConnection(100L)).isTrue();
    }

    @Test
    @DisplayName("마지막 done PubSub을 유실해도 DB 완료 상태 재연결은 done으로 종료한다")
    void recoverLostDoneFromCompletedDatabaseState() {
        ChatSseService service = service(true);
        Member member = createMember(1L);
        ChatRoom chatRoom = createChatRoom(10L, member);
        ChatMessage message = ChatMessage.createPendingUserText(chatRoom, "가을 니트를 추천해줘");
        ReflectionTestUtils.setField(message, "id", 100L);
        given(chatRoomRepository.findActiveById(10L)).willReturn(Optional.of(chatRoom));
        given(chatMessageRepository.findByIdAndChatRoomId(100L, 10L)).willReturn(Optional.of(message));

        ChatGenerationStatusResponse completed = org.mockito.Mockito.mock(ChatGenerationStatusResponse.class);
        ChatMessageDetailResponse aiMessage = org.mockito.Mockito.mock(ChatMessageDetailResponse.class);
        given(completed.getGenerationStatus()).willReturn(ChatGenerationStatus.COMPLETED);
        given(completed.getMessage()).willReturn(aiMessage);
        given(aiMessage.getContent()).willReturn("DB 최종 결과");
        given(aiMessage.getRecommendation()).willReturn(null);
        given(chatService.getGenerationStatus(1L, 10L, 100L)).willReturn(completed);

        service.connect(1L, 10L, 100L);

        assertThat(service.hasConnection(100L)).isFalse();
        verify(chatRedisSnapshotRepository, never()).findByMessageId(100L);
    }

    @Test
    @DisplayName("이미 종료된 연결에 늦게 도착한 Redis event는 무시한다")
    void ignoreLateRedisEventAfterConnectionCompleted() {
        ChatSseService service = service();

        service.complete(100L);
        service.handleRedisEvent(ChatRedisEvent.token(100L, 1L, "늦은 token"));

        assertThat(service.hasConnection(100L)).isFalse();
    }

    private Member createMember(Long memberId) {
        Member member = Member.create(
                "member" + memberId + "@lookddak.com",
                "encoded-password"
        );
        ReflectionTestUtils.setField(member, "id", memberId);
        return member;
    }

    private ChatSseService service() {
        return service(false);
    }

    private ChatSseService service(boolean redisEnabled) {
        return new ChatSseService(
                chatRoomRepository,
                chatMessageRepository,
                new ChatSseSnapshotStore(),
                chatService,
                new ChatRedisProperties(redisEnabled, "lookddak:chat"),
                chatRedisSnapshotRepository
        );
    }

    private ChatRoom createChatRoom(Long chatRoomId, Member member) {
        ChatRoom chatRoom = ChatRoom.create(
                member,
                "기존 채팅방",
                ChatSourceType.GENERAL,
                LocalDateTime.now().minusMinutes(1)
        );
        ReflectionTestUtils.setField(chatRoom, "id", chatRoomId);
        return chatRoom;
    }
}
