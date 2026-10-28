package com.youlai.boot.framework.security.token;

import com.youlai.boot.framework.security.session.RedisSessionStore;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Redis Token 是随机凭证，服务端通过摘要查找会话引用
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "security.token", name = "type", havingValue = "redis-token")
public class OpaqueAccessTokenCodec implements AccessTokenCodec {

    private final RedisSessionStore sessions;

    @Override
    public IssuedToken issue(String sessionId, Long userId, Instant issuedAt, Instant expiresAt) {
        String token = SecureTokenGenerator.generate();
        return new IssuedToken(token, new Reference(sessionId, userId, expiresAt), SecureTokenGenerator.digest(token));
    }

    @Override
    public Reference verify(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) {
            throw invalid();
        }
        Reference reference = sessions.findAccessToken(SecureTokenGenerator.digest(token));
        if (reference == null) {
            throw invalid();
        }
        return reference;
    }

    /**
     * 创建随机访问凭证无效异常
     */
    private static BadCredentialsException invalid() {
        return new BadCredentialsException("Invalid access token");
    }
}
