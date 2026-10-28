package com.youlai.boot.auth.session.service;

import com.youlai.boot.auth.oidc.service.OidcLogoutService;
import com.youlai.boot.framework.security.model.TokenPair;
import com.youlai.boot.framework.security.session.AuthenticatedSession;
import com.youlai.boot.framework.security.session.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * 登录会话的 HTTP 用例入口
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
@Service
@RequiredArgsConstructor
public class AuthSessionService {

    private final SessionService sessions;
    private final ObjectProvider<OidcLogoutService> oidcLogout;

    /**
     * 先撤销本地会话，再尝试退出认证中心
     */
    public String logout() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getDetails() instanceof AuthenticatedSession session)) {
            return null;
        }
        sessions.revokeSession(session.id());
        SecurityContextHolder.clearContext();
        var logout = oidcLogout.getIfAvailable();
        return logout == null ? null : logout.getLogoutUrl(session.context());
    }

    /**
     * 刷新当前登录会话
     */
    public TokenPair refreshToken(String refreshToken) {
        return sessions.refresh(refreshToken);
    }
}
