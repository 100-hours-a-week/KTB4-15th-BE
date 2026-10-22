package com.ktb.lookddak.domain.chat.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatSseSnapshotStoreTest {

    @Test
    @DisplayName("같은 사용자 메시지의 token은 하나의 snapshot에 수신 순서대로 누적된다")
    void appendTokensToSameSnapshot() {
        ChatSseSnapshotStore store = new ChatSseSnapshotStore();
        ChatSseSnapshotStore.SnapshotState state = store.getOrCreate(100L);

        synchronized (state.getLock()) {
            state.append("조건에 맞는 ");
            state.append("상품을 찾아봤어요.");
        }

        assertThat(store.getOrCreate(100L).content())
                .isEqualTo("조건에 맞는 상품을 찾아봤어요.");
    }

    @Test
    @DisplayName("작업 완료 후 snapshot을 제거하면 동일 메시지 ID도 새로운 누적 상태로 시작한다")
    void removeSnapshotAfterCompletion() {
        ChatSseSnapshotStore store = new ChatSseSnapshotStore();
        ChatSseSnapshotStore.SnapshotState previous = store.getOrCreate(100L);
        previous.append("이전 응답");

        store.remove(100L, previous);

        ChatSseSnapshotStore.SnapshotState current = store.getOrCreate(100L);
        assertThat(current).isNotSameAs(previous);
        assertThat(current.content()).isBlank();
    }
}
