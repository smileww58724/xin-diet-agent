package com.dietagent.agent.memory;

import com.dietagent.entity.ChatMessage;
import com.dietagent.repository.ChatMessageRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 对话记忆的持久化存储：MySQL 为权威数据，进程内 Caffeine 做读穿透缓存。
 * - 重启不丢失：历史从 chat_message 表重建；
 * - 多实例共享：任意实例写入落库，其他实例缓存过期后读到最新；
 * - 限量淘汰：每用户滑动窗口 20 条（写后裁剪），本地缓存限量 + 闲置淘汰。
 */
@Service
public class ChatMemoryStore {

    /** 与 ChatMessageRepository.findTop20 的固定窗口保持一致 */
    public static final int MAX_MESSAGES = 20;

    private static final long MAX_CACHED_USERS = 1000;
    private static final Duration EXPIRE_AFTER_ACCESS = Duration.ofHours(2);

    private final ChatMessageRepository repository;

    private final Cache<Long, ChatMemory> hotMemories = Caffeine.newBuilder()
            .maximumSize(MAX_CACHED_USERS)
            .expireAfterAccess(EXPIRE_AFTER_ACCESS)
            .build();

    public ChatMemoryStore(ChatMessageRepository repository) {
        this.repository = repository;
    }

    /** 取当前用户最近的消息快照（缓存未命中时从库重建） */
    public List<ChatMemory.ChatMessage> loadRecent(Long userId) {
        return hotMemories.get(userId, this::loadFromDb).snapshot();
    }

    /** 追加一条消息：落库（含裁剪）+ 回灌本地缓存 */
    @Transactional
    public void append(Long userId, String role, String content) {
        if (content == null || content.isBlank()) {
            return;
        }
        repository.save(ChatMessage.builder()
                .userId(userId)
                .role(role)
                .content(content)
                .build());
        repository.trimToRecent(userId, MAX_MESSAGES);
        hotMemories.get(userId, this::loadFromDb).addByRole(role, content);
    }

    /** 清空对话历史 */
    @Transactional
    public void clear(Long userId) {
        repository.deleteByUserId(userId);
        hotMemories.invalidate(userId);
    }

    private ChatMemory loadFromDb(Long userId) {
        // 防御性拷贝：仓库返回的列表实现不保证可变，reverse 前先包一层
        List<ChatMessage> rows = new ArrayList<>(repository.findTop20ByUserIdOrderByIdDesc(userId));
        Collections.reverse(rows);
        ChatMemory memory = new ChatMemory(userId);
        rows.forEach(row -> memory.addByRole(row.getRole(), row.getContent()));
        return memory;
    }
}
