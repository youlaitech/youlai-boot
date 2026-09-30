package com.youlai.boot.auth.session.service;

import cn.hutool.core.util.StrUtil;
import com.youlai.boot.framework.security.model.AuthenticationToken;
import com.youlai.boot.framework.security.token.TokenManager;
import com.youlai.boot.framework.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * 登录态服务
 *
 * @author Ray.Hao
 * @since 4.8.0
 */
@Service
@RequiredArgsConstructor
public class AuthSessionService {

    private final TokenManager tokenManager;

    /**
     * 注销登录
     */
    public void logout() {
        String token = SecurityUtils.getAccessToken();
        if (StrUtil.isNotBlank(token)) {
            tokenManager.invalidateToken(token);
            // 清除Security上下文
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * 刷新令牌
     *
     * @param refreshToken 刷新令牌
     * @return 新的访问令牌
     */
    public AuthenticationToken refreshToken(String refreshToken) {
        return tokenManager.refreshToken(refreshToken);
    }

}
