package com.dietagent.agent.memory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatMemoryTest {

    @Test
    @DisplayName("超过容量 20 条时先进先出截断")
    void fifoTruncation() {
        ChatMemory memory = new ChatMemory(1L);
        for (int i = 1; i <= 21; i++) {
            memory.addUserMessage("消息" + i);
        }

        List<ChatMemory.ChatMessage> snapshot = memory.snapshot();
        assertEquals(20, snapshot.size());
        // 第 1 条应被挤出，最旧的是第 2 条
        assertEquals("user", snapshot.get(0).getRole());
        assertEquals("消息2", snapshot.get(0).getContent());
        assertEquals("消息21", snapshot.get(snapshot.size() - 1).getContent());
    }

    @Test
    @DisplayName("消息按 user/assistant 顺序保留角色")
    void rolesPreserved() {
        ChatMemory memory = new ChatMemory(1L);
        memory.addUserMessage("我今天吃了什么");
        memory.addAssistantMessage("你今天吃了两个鸡蛋");

        List<ChatMemory.ChatMessage> snapshot = memory.snapshot();
        assertEquals(2, snapshot.size());
        assertEquals("user", snapshot.get(0).getRole());
        assertEquals("assistant", snapshot.get(1).getRole());
    }

    @Test
    @DisplayName("snapshot 是副本，外部修改不影响内部状态")
    void snapshotIsCopy() {
        ChatMemory memory = new ChatMemory(1L);
        memory.addUserMessage("hello");

        List<ChatMemory.ChatMessage> snapshot = memory.snapshot();
        snapshot.clear();

        assertEquals(1, memory.snapshot().size());
    }

    @Test
    @DisplayName("clear 清空全部历史")
    void clearEmpties() {
        ChatMemory memory = new ChatMemory(1L);
        memory.addUserMessage("a");
        memory.addAssistantMessage("b");

        memory.clear();

        assertTrue(memory.snapshot().isEmpty());
    }
}
