package com.youlai.boot.framework.web.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Redis 滑动窗口限流器
 *
 * @author Ray.Hao
 * @since 4.4.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisSlidingWindowRateLimiter {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final DefaultRedisScript<Long> SCRIPT;

    static {
        SCRIPT = new DefaultRedisScript<>();
        SCRIPT.setScriptText(
                "local key = KEYS[1] " +
                "local now = tonumber(ARGV[1]) " +
                "local window = tonumber(ARGV[2]) " +
                "local member = ARGV[3] " +
                "redis.call('ZREMRANGEBYSCORE', key, 0, now - window) " +
                "redis.call('ZADD', key, now, member) " +
                "redis.call('PEXPIRE', key, window + 1000) " +
                "return redis.call('ZCARD', key)"
        );
        SCRIPT.setResultType(Long.class);
    }

    /**
     * 记录一次请求并返回窗口内计数
     *
     * @param key      限流 Key（如 {@code rate_limit:ip:192.168.1.1}）
     * @param windowMs 窗口大小（毫秒）
     * @return 当前窗口内请求数（包含本次）；Redis 异常时返回 null，调用方按 Fail-Open 放行
     */
    public Long count(String key, long windowMs) {
        try {
            // 传 Long 而非 String，避免 Jackson 序列化器加双引号导致 Lua tonumber() 返回 nil
            return redisTemplate.execute(
                    SCRIPT,
                    List.of(key),
                    System.currentTimeMillis(),
                    windowMs,
                    UUID.randomUUID().toString()
            );
        } catch (Exception e) {
            log.warn("限流 Redis 异常，本次未计数（Fail-Open）  key={}", key, e);
            return null;
        }
    }
}
