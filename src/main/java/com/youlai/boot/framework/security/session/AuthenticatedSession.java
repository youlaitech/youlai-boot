package com.youlai.boot.framework.security.session;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.Instant;

/**
 * 已验证的会话身份与访问凭证期限
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
public record AuthenticatedSession(String id, SessionPrincipal principal, LoginContext context,
                                   Instant accessExpiresAt) {

    /**
     * 转换为携带会话详情的 Spring Security 认证结果
     */
    public Authentication toAuthentication() {
        var details = principal.toUserDetails();
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                details, null, details.getAuthorities());
        authentication.setDetails(this);
        return authentication;
    }
}
