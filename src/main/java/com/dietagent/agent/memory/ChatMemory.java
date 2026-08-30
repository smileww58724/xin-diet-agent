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

    /** 按角色追加消息（user / assistant），供持久化层回灌使用 */
    public void addByRole(String role, String content) {
        if ("assistant".equals(role)) {
            addAssistantMessage(content);
        } else {
            addUserMessage(content);
        }
    }

    /** 当前历史的只读快照，供组装 Spring AI messages 使用，避免暴露内部可变列表 */
    public List<ChatMessage> snapshot() {
        return new ArrayList<>(messages);
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
