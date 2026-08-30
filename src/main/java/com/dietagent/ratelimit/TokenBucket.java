package com.dietagent.ratelimit;

/** 简单令牌桶：以固定速率补充令牌，桶容量限制突发调用量 */
final class TokenBucket {

    private final double capacity;
    private final double refillPerNanos;
    private double tokens;
    private long lastRefillNanos;

    TokenBucket(int capacity, int refillPerMinute) {
        this.capacity = capacity;
        this.tokens = capacity;
        this.refillPerNanos = refillPerMinute / 60_000_000_000.0;
        this.lastRefillNanos = System.nanoTime();
    }

    synchronized boolean tryAcquire() {
        long now = System.nanoTime();
        tokens = Math.min(capacity, tokens + (now - lastRefillNanos) * refillPerNanos);
        lastRefillNanos = now;
        if (tokens >= 1) {
            tokens -= 1;
            return true;
        }
        return false;
    }
}
