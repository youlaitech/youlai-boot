package com.youlai.boot.support.realtime.security;

import com.youlai.boot.framework.security.token.SecureTokenGenerator;
import com.youlai.boot.support.realtime.model.CreateRealtimeTicketRequest;
import com.youlai.boot.support.realtime.model.RealtimeTicketResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * 签发并消费实时连接的一次性票据
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
@Service
@RequiredArgsConstructor
public class RealtimeTicketService {
    private static final Duration TICKET_TTL = Duration.ofSeconds(30);
    // 取出即删除，握手票据只能使用一次
    private static final DefaultRedisScript<String> CONSUME = new DefaultRedisScript<>(
            "local v=redis.call('GET',KEYS[1]); if v then redis.call('DEL',KEYS[1]); end; return v",
            String.class);
    private final StringRedisTemplate redis;
    private final RealtimeCredentialVerifier verifier;
    private final JsonMapper mapper = JsonMapper.builder().build();

    /**
     * 校验访问凭证后签发短期一次性票据
     */
    public RealtimeTicketResponse create(String accessToken, CreateRealtimeTicketRequest request) {
        var identity = verifier.verify(accessToken);
        checkTopics(request.topics(), identity.presenceAllowed());
        String ticket = SecureTokenGenerator.generate();
        TicketGrant grant = new TicketGrant(accessToken, identity.userId(), request.clientInstanceId(),
                request.platform(), request.topics(), Instant.now().plus(TICKET_TTL).toEpochMilli());
        redis.opsForValue().set(key(ticket), mapper.writeValueAsString(grant), TICKET_TTL);
        return new RealtimeTicketResponse(ticket);
    }

    /**
     * 原子取出并删除后复核到期与身份，已取出的票据不回滚
     */
    public VerifiedTicket consume(String ticket) {
        if (ticket == null || !ticket.matches("[A-Za-z0-9_-]{43,256}")) {
            throw invalid();
        }
        String json = redis.execute(CONSUME, List.of(key(ticket)));
        if (json == null) {
            throw invalid();
        }
        TicketGrant grant = mapper.readValue(json, TicketGrant.class);
        if (grant.expiresAtMillis() <= System.currentTimeMillis()) {
            throw invalid();
        }
        var identity = verifier.verify(grant.accessToken());
        if (!grant.userId().equals(identity.userId())) {
            throw invalid();
        }
        checkTopics(grant.topics(), identity.presenceAllowed());
        return new VerifiedTicket(grant, identity.credentialExpiresAt());
    }

    /**
     * 在线统计主题需要对应的查看权限
     */
    private void checkTopics(Set<String> topics, boolean presenceAllowed) {
        if (topics.contains("presence") && !presenceAllowed) {
            throw new AccessDeniedException("Presence topic forbidden");
        }
    }

    /**
     * 通过票据摘要构造 Redis 存储键
     */
    private String key(String ticket) {
        // Redis 键只保存摘要，不暴露可用于握手的票据
        return "realtime:ticket:" + SecureTokenGenerator.digest(ticket);
    }

    /**
     * 创建实时连接票据无效异常
     */
    private BadCredentialsException invalid() {
        return new BadCredentialsException("Invalid realtime ticket");
    }

    /**
     * 仅限服务器内部，禁止把该对象放入响应或日志
     */
    public record TicketGrant(String accessToken, Long userId, String clientInstanceId,
                              String platform, Set<String> topics, long expiresAtMillis) {
        /**
         * 固定本次连接的订阅集合
         */
        public TicketGrant {
            topics = Set.copyOf(topics);
        }

        @Override
        public String toString() {
            return "TicketGrant[REDACTED]";
        }
    }

    /**
     * 本次握手已验证的票据及访问凭证到期时间
     */
    public record VerifiedTicket(TicketGrant grant, Instant credentialExpiresAt) {
    }
}
