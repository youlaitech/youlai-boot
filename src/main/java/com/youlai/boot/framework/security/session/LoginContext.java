package com.youlai.boot.framework.security.session;

import java.util.Objects;

/**
 * 登录来源与外部身份，随本地会话保存
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
public record LoginContext(Source source, String provider, String issuer, String subject, String idToken) {

    /**
     * 登录身份来源
     */
    public enum Source {
        LOCAL,
        OIDC
    }

    public LoginContext {
        Objects.requireNonNull(source, "source");
        if (source == Source.OIDC && (provider == null || provider.isBlank()
                || issuer == null || issuer.isBlank() || subject == null || subject.isBlank())) {
            throw new IllegalArgumentException("OIDC context requires provider, issuer and subject");
        }
        if (source == Source.LOCAL && (provider != null || issuer != null || subject != null || idToken != null)) {
            throw new IllegalArgumentException("Local context cannot contain an external identity");
        }
    }

    /**
     * 创建本地登录上下文
     */
    public static LoginContext local() {
        return new LoginContext(Source.LOCAL, null, null, null, null);
    }

    /**
     * 保存 OIDC 身份，ID Token 用于认证中心登出
     */
    public static LoginContext oidc(String provider, String issuer, String subject, String idToken) {
        return new LoginContext(Source.OIDC, provider, issuer, subject, idToken);
    }

    /**
     * 日志不输出 ID Token
     */
    @Override
    public String toString() {
        return "LoginContext[source=" + source + ", provider=" + provider
                + ", issuer=" + issuer + ", subject=" + subject + "]";
    }
}
