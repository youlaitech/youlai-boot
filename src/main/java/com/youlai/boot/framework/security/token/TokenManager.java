package com.youlai.boot.framework.security.token;


import com.youlai.boot.framework.security.model.AuthenticationToken;
import org.springframework.security.core.Authentication;

/**
 *  Token 管理器
 *
 * @author Ray.Hao
 * @since 2.16.0
 */
public interface TokenManager {

    /**
     * 生成认证 Token
     *
     * @param authentication 用户认证信息
     * @return 认证 Token 响应
     */
    AuthenticationToken generateToken(Authentication authentication);

    /**
     * 解析 Token 获取认证信息
     *
     * @param token  Token
     * @return 用户认证信息
     */
    Authentication parseToken(String token);

    /**
     * 校验 Token 是否有效
     *
     * @param token JWT Token
     * @return 是否有效
     */
    boolean validateToken(String token);

    /**
     * 校验 刷新 Token 是否有效
     *
     * @param refreshToken JWT Token
     * @return 是否有效
     */
    boolean validateRefreshToken(String refreshToken);

    /**
     *  刷新 Token
     *
     * @param token 刷新令牌
     * @return 认证 Token 响应
     */
    AuthenticationToken refreshToken(String token);

    /**
     * 令 Token 失效
     *
     * @param token Token
     */
    default void invalidateToken(String token) {
    }

    /**
     * 使指定用户的所有会话失效
     *
     * @param userId 用户ID
     */
    default void invalidateUserSessions(Long userId) {
        // 默认空实现，由具体 TokenManager 决定是否支持按用户下线
    }

}
