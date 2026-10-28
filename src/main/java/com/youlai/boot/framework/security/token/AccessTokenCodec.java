package com.youlai.boot.framework.security.token;

import java.time.Instant;

/**
 * 访问令牌格式策略；会话生命周期由 SessionService 管理
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
public interface AccessTokenCodec {

    /**
     * 签发引用指定会话的访问凭证
     */
    IssuedToken issue(String sessionId, Long userId, Instant issuedAt, Instant expiresAt);

    /**
     * 校验签名或凭证索引，返回引用；账号与会话状态仍须由 SessionService 校验
     */
    Reference verify(String token);

    /**
     * 访问凭证对应的会话、用户与截止时间
     */
    record Reference(String sessionId, Long userId, Instant expiresAt) {
    }

    /**
     * lookupDigest 仅用于不透明令牌的 Redis 索引
     */
    record IssuedToken(String value, Reference reference, String lookupDigest) {
        @Override
        public String toString() {
            return "IssuedToken[reference=" + reference + ", value=REDACTED]";
        }
    }
}
