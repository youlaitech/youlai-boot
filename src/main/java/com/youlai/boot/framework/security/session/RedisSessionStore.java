package com.youlai.boot.framework.security.session;

import com.youlai.boot.framework.security.token.AccessTokenCodec;
import com.youlai.boot.framework.security.token.SecureTokenGenerator;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

/**
 * 保存会话和凭证摘要，使用原子脚本处理登录与刷新竞争
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
@Component
public class RedisSessionStore {

    private static final String PREFIX = "auth:v3:";
    private static final long REFRESH_OVERLAP_MILLIS = 5_000;
    // 登录发布、单设备替换和不透明令牌索引必须一起完成
    private static final DefaultRedisScript<Long> CREATE = new DefaultRedisScript<>("""
            local generation = redis.call('GET', KEYS[2])
            if generation ~= ARGV[1] or redis.call('EXISTS', KEYS[1]) == 1 then
                return 0
            end
            redis.call('SET', KEYS[2], ARGV[2])
            redis.call('SET', KEYS[1], ARGV[3], 'PX', ARGV[4])
            if ARGV[5] ~= '' then
                redis.call('SET', KEYS[3], ARGV[5], 'PX', ARGV[6])
            end
            return 1
            """, Long.class);
    // 刷新轮换：会话代次、刷新凭证与令牌索引一次写入
    private static final DefaultRedisScript<Long> ROTATE = new DefaultRedisScript<>("""
            local raw = redis.call('GET', KEYS[1])
            if not raw then return 0 end
            local session = cjson.decode(raw)
            local now = tonumber(ARGV[3])
            if redis.call('GET', KEYS[2]) ~= session.generation or tonumber(session.expiresAt) <= now then
                return 0
            end
            if session.refreshDigest ~= ARGV[1] then
                local consumedAt = redis.call('GET', KEYS[3])
                -- 短时间内的并发刷新只拒绝重复请求；较晚的已用凭证重放撤销整条会话
                if consumedAt and now - tonumber(consumedAt) > tonumber(ARGV[7]) then
                    redis.call('DEL', KEYS[1])
                end
                return 0
            end
            if ARGV[2] == '' then return 0 end
            redis.call('SET', KEYS[3], ARGV[3], 'PX', ARGV[4])
            redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[4])
            if ARGV[5] ~= '' then
                redis.call('SET', KEYS[4], ARGV[5], 'PX', ARGV[6])
            end
            return 1
            """, Long.class);

    private final StringRedisTemplate redis;
    private final JsonMapper json;

    public RedisSessionStore(StringRedisTemplate redis, JsonMapper json) {
        this.redis = redis;
        this.json = json;
    }

    /**
     * 读取用户会话版本，首次访问时生成
     */
    public String currentGeneration(Long userId) {
        return execute(() -> {
            String key = generationKey(userId);
            redis.opsForValue().setIfAbsent(key, SecureTokenGenerator.generate());
            String generation = redis.opsForValue().get(key);
            if (generation == null) {
                throw unavailable();
            }
            return generation;
        });
    }

    /**
     * 更换用户会话版本，使既有会话失效
     */
    public void revokeUserSessions(Long userId) {
        execute(() -> {
            redis.opsForValue().set(generationKey(userId), SecureTokenGenerator.generate());
            return null;
        });
    }

    /**
     * 删除指定会话
     */
    public void revokeSession(String sessionId) {
        execute(() -> redis.delete(sessionKey(sessionId)));
    }

    /**
     * 原子校验用户版本并保存会话与访问凭证索引
     */
    public boolean create(LoginSession session, String expectedGeneration,
                          AccessTokenCodec.IssuedToken accessToken, Instant now) {
        return execute(() -> Long.valueOf(1).equals(redis.execute(CREATE,
                List.of(sessionKey(session.id()), generationKey(session.principal().userId()), accessKey(accessToken)),
                expectedGeneration, session.generation(), json.writeValueAsString(session),
                remaining(session.expiresAt(), now), accessJson(accessToken), accessRemaining(accessToken, now))));
    }

    /**
     * 读取未过期且用户版本仍有效的会话
     */
    public LoginSession findActive(String sessionId, Instant now) {
        return execute(() -> {
            String raw = redis.opsForValue().get(sessionKey(sessionId));
            if (raw == null) {
                return null;
            }
            LoginSession session = json.readValue(raw, LoginSession.class);
            String generation = redis.opsForValue().get(generationKey(session.principal().userId()));
            if (!session.generation().equals(generation) || session.expiresAt() <= now.toEpochMilli()) {
                return null;
            }
            return session;
        });
    }

    /**
     * 通过随机访问凭证的摘要查找会话引用
     */
    public AccessTokenCodec.Reference findAccessToken(String digest) {
        return execute(() -> {
            String raw = redis.opsForValue().get(PREFIX + "access:" + digest);
            return raw == null ? null : json.readValue(raw, AccessTokenCodec.Reference.class);
        });
    }

    /**
     * 原子轮换刷新凭证；替换值为空时仅检查旧凭证重放
     */
    public boolean rotate(LoginSession previous, String presentedDigest, LoginSession replacement,
                          AccessTokenCodec.IssuedToken accessToken, Instant now) {
        return execute(() -> Long.valueOf(1).equals(redis.execute(ROTATE,
                List.of(sessionKey(previous.id()), generationKey(previous.principal().userId()),
                        PREFIX + "refresh-used:" + previous.id() + ":" + presentedDigest,
                        accessToken == null ? sessionKey(previous.id()) : accessKey(accessToken)),
                presentedDigest, replacement == null ? "" : json.writeValueAsString(replacement),
                Long.toString(now.toEpochMilli()), remaining(previous.expiresAt(), now),
                accessToken == null ? "" : accessJson(accessToken),
                accessToken == null ? "1" : accessRemaining(accessToken, now),
                Long.toString(REFRESH_OVERLAP_MILLIS))));
    }

    /**
     * 序列化随机令牌的会话引用，JWT 不保存该索引
     */
    private String accessJson(AccessTokenCodec.IssuedToken token) {
        return token.lookupDigest() == null ? "" : json.writeValueAsString(token.reference());
    }

    /**
     * 构造随机令牌索引键，JWT 使用会话键占位
     */
    private static String accessKey(AccessTokenCodec.IssuedToken token) {
        return token.lookupDigest() == null ? sessionKey(token.reference().sessionId())
                : PREFIX + "access:" + token.lookupDigest();
    }

    /**
     * 计算访问凭证剩余有效毫秒数
     */
    private static String accessRemaining(AccessTokenCodec.IssuedToken token, Instant now) {
        return remaining(token.reference().expiresAt().toEpochMilli(), now);
    }

    /**
     * 计算剩余有效毫秒数，最小值为 1
     */
    private static String remaining(long expiresAt, Instant now) {
        return Long.toString(Math.max(1, expiresAt - now.toEpochMilli()));
    }

    /**
     * 构造会话存储键
     */
    private static String sessionKey(String sessionId) {
        return PREFIX + "session:" + sessionId;
    }

    /**
     * 构造用户会话版本键
     */
    private static String generationKey(Long userId) {
        return PREFIX + "user-generation:" + userId;
    }

    /**
     * 执行存储操作，将存储和解析故障转换为认证服务异常
     */
    private static <T> T execute(Supplier<T> operation) {
        try {
            return operation.get();
        } catch (DataAccessException exception) {
            throw new AuthenticationServiceException("Session storage is unavailable", exception);
        } catch (tools.jackson.core.JacksonException exception) {
            throw new AuthenticationServiceException("Session data cannot be read", exception);
        }
    }

    /**
     * 创建会话存储不可用异常
     */
    private static AuthenticationServiceException unavailable() {
        return new AuthenticationServiceException("Session storage is unavailable");
    }
}
