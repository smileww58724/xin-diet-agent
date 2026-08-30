package com.dietagent.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 对话接口的用户级限流（令牌桶）。
 * 大模型调用按 token 计费且单次耗时以秒计，不设防的调用频率
 * 等于把 API 账单和上游配额暴露给任何一个失控的前端循环。
 */
@Component
public class ChatRateLimiter {

    private final Cache<Long, TokenBucket> buckets;
    private final int capacity;
    private final int refillPerMinute;

    public ChatRateLimiter(
            @Value("${agent.chat.rate-limit.capacity:10}") int capacity,
            @Value("${agent.chat.rate-limit.refill-per-minute:10}") int refillPerMinute) {
        this.capacity = capacity;
        this.refillPerMinute = refillPerMinute;
        this.buckets = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterAccess(Duration.ofHours(1))
                .build();
    }

    /** 尝试为用户获取一次对话额度；桶按用户惰性创建，容量与速率可经配置调整 */
    public boolean tryAcquire(Long userId) {
        return buckets.get(userId, id -> new TokenBucket(capacity, refillPerMinute)).tryAcquire();
    }
}
