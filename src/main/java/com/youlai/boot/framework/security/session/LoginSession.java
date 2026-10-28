package com.youlai.boot.framework.security.session;

/**
 * Redis 中的会话记录。expiresAt 为固定截止时间，刷新不会延长它
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
public record LoginSession(String id, String generation, SessionPrincipal principal, LoginContext context,
                           String refreshDigest, long createdAt, long expiresAt) {
}
