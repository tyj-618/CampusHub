package com.tyj.campushub.post;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

@Component
@Profile("redis")
public class RedisHotPostCache implements HotPostCache {

    private static final String KEY_PREFIX = "campushub:post:hot:";
    private static final String LOCK_KEY_PREFIX = "campushub:lock:post:hot:";
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);
    private static final long LOCK_WAIT_MILLIS = 80;
    private static final TypeReference<List<PostHotItemResponse>> HOT_POST_LIST_TYPE = new TypeReference<>() {
    };

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl;
    private final long ttlJitterSeconds;

    public RedisHotPostCache(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper,
                             @Value("${campushub.cache.hot-post-ttl-seconds:300}") long ttlSeconds,
                             @Value("${campushub.cache.hot-post-ttl-jitter-seconds:60}") long ttlJitterSeconds) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.ttl = Duration.ofSeconds(ttlSeconds);
        this.ttlJitterSeconds = Math.max(ttlJitterSeconds, 0);
    }

    @Override
    public Optional<List<PostHotItemResponse>> get(int limit, Long categoryId) {
        String json = stringRedisTemplate.opsForValue().get(buildKey(limit, categoryId));
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(json, HOT_POST_LIST_TYPE));
        } catch (JsonProcessingException exception) {
            stringRedisTemplate.delete(buildKey(limit, categoryId));
            return Optional.empty();
        }
    }

    @Override
    public void put(int limit, Long categoryId, List<PostHotItemResponse> hotPosts) {
        try {
            String json = objectMapper.writeValueAsString(hotPosts);
            stringRedisTemplate.opsForValue().set(buildKey(limit, categoryId), json, ttlWithJitter());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("热门帖子缓存序列化失败", exception);
        }
    }

    @Override
    public List<PostHotItemResponse> getOrLoad(int limit, Long categoryId, Supplier<List<PostHotItemResponse>> loader) {
        Optional<List<PostHotItemResponse>> cached = get(limit, categoryId);
        if (cached.isPresent()) {
            return cached.get();
        }

        String lockKey = buildLockKey(limit, categoryId);
        String lockValue = UUID.randomUUID().toString();
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, LOCK_TTL);
        if (Boolean.TRUE.equals(locked)) {
            try {
                List<PostHotItemResponse> hotPosts = loader.get();
                put(limit, categoryId, hotPosts);
                return hotPosts;
            } finally {
                releaseLock(lockKey, lockValue);
            }
        }

        sleepBriefly();
        return get(limit, categoryId).orElseGet(loader);
    }

    private String buildKey(int limit, Long categoryId) {
        String categoryPart = categoryId == null ? "all" : categoryId.toString();
        return KEY_PREFIX + categoryPart + ":limit:" + limit;
    }

    private String buildLockKey(int limit, Long categoryId) {
        String categoryPart = categoryId == null ? "all" : categoryId.toString();
        return LOCK_KEY_PREFIX + categoryPart + ":limit:" + limit;
    }

    private Duration ttlWithJitter() {
        if (ttlJitterSeconds == 0) {
            return ttl;
        }
        long jitter = ThreadLocalRandom.current().nextLong(ttlJitterSeconds + 1);
        return ttl.plusSeconds(jitter);
    }

    private void releaseLock(String lockKey, String lockValue) {
        String currentValue = stringRedisTemplate.opsForValue().get(lockKey);
        if (lockValue.equals(currentValue)) {
            stringRedisTemplate.delete(lockKey);
        }
    }

    private void sleepBriefly() {
        try {
            Thread.sleep(LOCK_WAIT_MILLIS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
