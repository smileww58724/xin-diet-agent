package com.dietagent.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

/**
 * 缓存定位：营养汇总类查询纯做优化——同一份数据在 AI 对话、日报、周报间反复读取。
 * Redis 只承担 60 秒短 TTL 的热点缓存；连接失败时由 errorHandler 降级直查数据库，
 * 不让缓存故障影响业务可用性（部署环境没有 Redis 也能正常运行）。
 */
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    public static final String NUTRITION_SUMMARY = "nutritionSummary";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(60))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new DegradingCacheErrorHandler();
    }

    /** 缓存读写/失效失败一律记日志放行，等效于跳过缓存层 */
    @Slf4j
    static class DegradingCacheErrorHandler extends SimpleCacheErrorHandler {

        @Override
        public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
            log.warn("缓存读取失败（cache={} key={}），降级直查数据库: {}", cache.getName(), key, e.getMessage());
        }

        @Override
        public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
            log.warn("缓存写入失败（cache={} key={}）: {}", cache.getName(), key, e.getMessage());
        }

        @Override
        public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
            log.warn("缓存失效失败（cache={} key={}）: {}", cache.getName(), key, e.getMessage());
        }

        @Override
        public void handleCacheClearError(RuntimeException e, Cache cache) {
            log.warn("缓存清空失败（cache={}）: {}", cache.getName(), e.getMessage());
        }
    }
}
