package com.ktb.lookddak.domain.chat.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class ChatSseSnapshotStore {

    private final ConcurrentMap<Long, SnapshotState> states =
            new ConcurrentHashMap<>();

    public SnapshotState getOrCreate(Long userMessageId) {
        return states.computeIfAbsent(userMessageId, key -> new SnapshotState());
    }

    public void remove(Long userMessageId, SnapshotState state) {
        states.remove(userMessageId, state);
    }

    static class SnapshotState {

        private final Object lock = new Object();
        private final StringBuilder content = new StringBuilder();

        Object getLock() {
            return lock;
        }

        void append(String token) {
            content.append(token);
        }

        String content() {
            return content.toString();
        }
    }
}
