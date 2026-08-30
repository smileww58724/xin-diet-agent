package com.dietagent.agent.memory;

import com.dietagent.entity.ChatMessage;
import com.dietagent.repository.ChatMessageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatMemoryStoreTest {

    @Mock
    private ChatMessageRepository repository;

    private ChatMemoryStore store() {
        return new ChatMemoryStore(repository);
    }

    private ChatMessage row(long id, String role, String content) {
        return ChatMessage.builder().id(id).userId(1L).role(role).content(content).build();
    }

    @Test
    @DisplayName("追加消息：落库 + 裁剪到最近 20 条 + 回灌缓存")
    void appendPersistsAndTrims() {
        ChatMemoryStore store = store();
        store.append(1L, "user", "我今天吃了什么");

        verify(repository).save(any(ChatMessage.class));
        verify(repository).trimToRecent(1L, ChatMemoryStore.MAX_MESSAGES);
        assertTrue(store.loadRecent(1L).stream().anyMatch(m -> "我今天吃了什么".equals(m.getContent())));
    }

    @Test
    @DisplayName("读穿透：同一用户连续读取只查一次库")
    void loadRecentHitsCache() {
        when(repository.findTop20ByUserIdOrderByIdDesc(1L)).thenReturn(List.of());

        ChatMemoryStore store = store();
        store.loadRecent(1L);
        store.loadRecent(1L);
        store.loadRecent(1L);

        verify(repository, times(1)).findTop20ByUserIdOrderByIdDesc(1L);
    }

    @Test
    @DisplayName("从库重建时按写入正序回放（倒序查询结果反转）")
    void loadFromDbRestoresOrder() {
        when(repository.findTop20ByUserIdOrderByIdDesc(1L)).thenReturn(
                List.of(row(3, "assistant", "回答"), row(2, "user", "追问"), row(1, "user", "提问")));

        List<ChatMemory.ChatMessage> snapshot = store().loadRecent(1L);

        assertEquals(3, snapshot.size());
        assertEquals("提问", snapshot.get(0).getContent());
        assertEquals("user", snapshot.get(0).getRole());
        assertEquals("回答", snapshot.get(2).getContent());
        assertEquals("assistant", snapshot.get(2).getRole());
    }

    @Test
    @DisplayName("空内容不落库（取消场景的部分回复可能为空）")
    void appendSkipsBlank() {
        store().append(1L, "assistant", "  ");

        verify(repository, never()).save(any(ChatMessage.class));
        verify(repository, never()).trimToRecent(any(), anyInt());
    }

    @Test
    @DisplayName("清空：删库 + 缓存失效，下次读取重新穿透")
    void clearDeletesAndInvalidates() {
        when(repository.findTop20ByUserIdOrderByIdDesc(1L)).thenReturn(List.of());

        store().clear(1L);
        store().loadRecent(1L);

        verify(repository).deleteByUserId(1L);
        // 缓存已失效，loadRecent 需要重新查库
        verify(repository).findTop20ByUserIdOrderByIdDesc(1L);
    }

    @Test
    @DisplayName("裁剪参数与固定窗口常量一致")
    void trimUsesConfiguredWindow() {
        ChatMemoryStore store = store();
        store.append(1L, "user", "hello");

        verify(repository).trimToRecent(eq(1L), eq(20));
    }
}
