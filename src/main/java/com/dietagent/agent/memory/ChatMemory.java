package com.dietagent.agent.memory;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Data
public class ChatMemory {

    private static final int MAX_HISTORY = 20;

    private final Long userId;
    private final LinkedList<ChatMessage> messages;

    public ChatMemory(Long userId) {
        this.userId = userId;
        this.messages = new LinkedList<>();
    }

    public void addUserMessage(String content) {
        if (messages.size() >= MAX_HISTORY) {
            messages.removeFirst();
        }
        messages.add(new ChatMessage("user", content));
    }

    public void addAssistantMessage(String content) {
        if (messages.size() >= MAX_HISTORY) {
            messages.removeFirst();
        }
        messages.add(new ChatMessage("assistant", content));
    }

    public List<ChatMessage> getRecentMessages(int count) {
        int size = messages.size();
        int start = Math.max(0, size - count);
        return new ArrayList<>(messages.subList(start, size));
    }

    public String getConversationHistory() {
        StringBuilder sb = new StringBuilder();
        for (ChatMessage msg : messages) {
            sb.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
        }
        return sb.toString();
    }

    public void clear() {
        messages.clear();
    }

    @Data
    public static class ChatMessage {
        private final String role;
        private final String content;

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
