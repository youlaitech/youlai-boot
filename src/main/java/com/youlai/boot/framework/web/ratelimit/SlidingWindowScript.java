package com.youlai.boot.framework.web.ratelimit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.UUID;

/**
 * 滑动窗口限流 Lua 脚本（共享工具）
 * <p>
 * 基于 Redis Sorted Set 实现原子化的滑动窗口计数：
 * <ol>
 *   <li>ZREMRANGEBYSCORE 清除窗口外旧请求</li>
 *   <li>ZADD 添加当前请求</li>
 *   <li>ZCARD 统计窗口内请求数</li>
 *   <li>PEXPIRE 设置 Key 过期（窗口 + 1s 冗余）</li>
 * </ol>
 * 一次网络往返完成全部操作，避免多次 Redis 调用之间的竞态条件。
 * </p>
 *
 * @author Ray.Hao
 * @since 4.4.0
 */
public final class SlidingWindowScript {

    private SlidingWindowScript() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

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
     * 执行滑动窗口计数
     *
     * @param redisTemplate Redis 模板
     * @param key           限流 Key（如 {@code rate_limit:ip:192.168.1.1}）
     * @param windowMs      窗口大小（毫秒）
     * @return 当前窗口内请求数（包含本次）
     */
    public static Long execute(RedisTemplate<String, Object> redisTemplate,
                               String key,
                               long windowMs) {
        // 传 Long 而非 String，避免 Jackson 序列化器加双引号导致 Lua tonumber() 返回 nil
        return redisTemplate.execute(
                SCRIPT,
                Collections.singletonList(key),
                System.currentTimeMillis(),
                windowMs,
                UUID.randomUUID().toString()
        );
    }

}
