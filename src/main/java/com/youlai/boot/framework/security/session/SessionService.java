package com.youlai.boot.framework.security.session;

import com.youlai.boot.common.exception.TokenInvalidException;
import com.youlai.boot.common.result.ResultCode;
import com.youlai.boot.framework.security.config.SecurityProperties;
import com.youlai.boot.framework.security.model.SecurityUser;
import com.youlai.boot.framework.security.model.SecurityUserDetails;
import com.youlai.boot.framework.security.model.TokenPair;
import com.youlai.boot.framework.security.port.UserAuthenticationPort;
import com.youlai.boot.framework.security.token.AccessTokenCodec;
import com.youlai.boot.framework.security.token.SecureTokenGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * 管理登录会话的创建、校验、刷新与撤销
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
@Service
@RequiredArgsConstructor
public class SessionService {

    private final RedisSessionStore store;
    private final AccessTokenCodec tokens;
    private final SecurityProperties properties;
    private final Clock clock;
    // 延迟解析业务端口，避免用户管理服务与会话服务形成构造依赖环
    private final ObjectProvider<UserAuthenticationPort> users;
    private final ObjectProvider<SessionValidator> validators;

    /**
     * 创建登录会话，并限制其最长有效期
     */
    public TokenPair create(Authentication authentication) {
        return create(authentication, LoginContext.local());
    }

    /**
     * 创建登录会话，并限制其最长有效期
     */
    public TokenPair create(Authentication authentication, LoginContext context) {
        if (!authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof SecurityUserDetails principal)
                || principal.getUserId() == null) {
            throw invalidAccess();
        }

        Instant now = now();
        Instant expiresAt = now.plus(properties.getToken().getRefreshTokenTimeToLive());
        if (!expiresAt.isAfter(now)) {
            throw invalidAccess();
        }

        String sessionId = SecureTokenGenerator.generate();
        String secret = SecureTokenGenerator.generate();
        var access = tokens.issue(sessionId, principal.getUserId(), now, accessExpiry(now, expiresAt));
        // 微信注册等事务提交后才发布会话；回滚不会留下可用凭证
        afterCommit(() -> publish(sessionId, principal.getUserId(), context, secret, now, expiresAt, access));
        return pair(access, sessionId, secret, now);
    }

    /**
     * 校验访问凭证、本地会话与外部登录来源
     */
    public AuthenticatedSession authenticate(String accessToken) {
        var reference = tokens.verify(accessToken);
        Instant now = now();
        if (!reference.expiresAt().isAfter(now)) {
            throw invalidAccess();
        }
        LoginSession session = store.findActive(reference.sessionId(), now);
        if (session == null || !session.principal().userId().equals(reference.userId())) {
            throw invalidAccess();
        }
        validateSource(session);
        return new AuthenticatedSession(session.id(), session.principal(), session.context(), reference.expiresAt());
    }

    /**
     * 每次刷新轮换凭证，并重新装载账号状态及权限
     */
    public TokenPair refresh(String refreshToken) {
        if (refreshToken == null || !refreshToken.matches("rt\\.[A-Za-z0-9_-]{43}\\.[A-Za-z0-9_-]{43}")) {
            throw invalidRefresh();
        }
        String[] parts = refreshToken.split("\\.");
        Instant now = now();
        LoginSession previous = store.findActive(parts[1], now);
        if (previous == null) {
            throw invalidRefresh();
        }
        String digest = SecureTokenGenerator.digest(parts[2]);
        if (!SecureTokenGenerator.matches(previous.refreshDigest(), digest)) {
            store.rotate(previous, digest, null, null, now);
            throw invalidRefresh();
        }
        try {
            validateSource(previous);
            SessionPrincipal principal = loadPrincipal(previous.principal().userId());
            String secret = SecureTokenGenerator.generate();
            var replacement = new LoginSession(previous.id(), previous.generation(), principal, previous.context(),
                    SecureTokenGenerator.digest(secret), previous.createdAt(), previous.expiresAt());
            var access = tokens.issue(previous.id(), principal.userId(), now,
                    accessExpiry(now, Instant.ofEpochMilli(previous.expiresAt())));
            if (!store.rotate(previous, digest, replacement, access, now)) {
                throw invalidRefresh();
            }
            return pair(access, previous.id(), secret, now);
        } catch (BadCredentialsException exception) {
            store.revokeSession(previous.id());
            throw invalidRefresh();
        }
    }

    /**
     * 撤销指定登录会话
     */
    public void revokeSession(String sessionId) {
        store.revokeSession(sessionId);
    }

    /**
     * 数据库事务提交成功后撤销，避免回滚操作误踢用户
     */
    public void revokeUserSessions(Long userId) {
        afterCommit(() -> store.revokeUserSessions(userId));
    }

    /**
     * 检查用户版本并发布会话，竞争失败时有限重试
     */
    private void publish(String sessionId, Long userId, LoginContext context, String secret,
                         Instant createdAt, Instant expiresAt, AccessTokenCodec.IssuedToken access) {
        for (int attempt = 0; attempt < 3; attempt++) {
            String expectedGeneration = store.currentGeneration(userId);
            String generation = properties.getToken().isAllowMultiLogin()
                    ? expectedGeneration : SecureTokenGenerator.generate();
            var session = new LoginSession(sessionId, generation, loadPrincipal(userId), context,
                    SecureTokenGenerator.digest(secret), createdAt.toEpochMilli(), expiresAt.toEpochMilli());
            validateSource(session);
            Instant now = now();
            if (!access.reference().expiresAt().isAfter(now)) {
                throw invalidAccess();
            }
            if (store.create(session, expectedGeneration, access, now)) {
                return;
            }
        }
        throw new BadCredentialsException("Account state changed during login; retry login");
    }

    /**
     * 重新加载有效账号及其权限
     */
    private SessionPrincipal loadPrincipal(Long userId) {
        SecurityUser user;
        try {
            user = users.getObject().getAuthInfoByUserId(userId);
        } catch (DataAccessException exception) {
            throw new AuthenticationServiceException("Account storage is unavailable", exception);
        }
        if (user == null || !userId.equals(user.getUserId()) || !Integer.valueOf(1).equals(user.getStatus())) {
            throw invalidAccess();
        }
        return SessionPrincipal.from(user);
    }

    /**
     * 校验外部登录来源；对应模块关闭时拒绝认证
     */
    private void validateSource(LoginSession session) {
        if (session.context().source() == LoginContext.Source.LOCAL) {
            return;
        }
        var validator = validators.orderedStream()
                .filter(candidate -> candidate.source() == session.context().source())
                .findFirst().orElseThrow(SessionService::invalidAccess);
        validator.validate(session);
    }

    /**
     * 计算不超过会话期限的访问凭证截止时间
     */
    private Instant accessExpiry(Instant now, Instant sessionExpiry) {
        Instant accessExpiry = now.plus(properties.getToken().getAccessTokenTimeToLive());
        return accessExpiry.isBefore(sessionExpiry) ? accessExpiry : sessionExpiry;
    }

    /**
     * 组合访问令牌、刷新凭证与剩余有效秒数
     */
    private static TokenPair pair(AccessTokenCodec.IssuedToken access, String sessionId, String secret, Instant now) {
        return TokenPair.bearer(access.value(), "rt." + sessionId + "." + secret,
                Duration.between(now, access.reference().expiresAt()).toSeconds());
    }

    /**
     * 获取精确到秒的认证时间
     */
    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.SECONDS);
    }

    /**
     * 事务提交后执行操作，无事务时立即执行
     */
    private static void afterCommit(Runnable operation) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            operation.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                operation.run();
            }
        });
    }

    /**
     * 创建会话无效异常
     */
    private static BadCredentialsException invalidAccess() {
        return new BadCredentialsException("Invalid session");
    }

    /**
     * 创建刷新凭证无效异常
     */
    private static TokenInvalidException invalidRefresh() {
        return new TokenInvalidException(ResultCode.REFRESH_TOKEN_INVALID);
    }
}
