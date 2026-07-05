package com.youlai.boot.framework.security.token;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.constant.SecurityConstants;
import com.youlai.boot.common.result.ResultCode;
import com.youlai.boot.framework.security.config.SecurityProperties;
import com.youlai.boot.framework.security.exception.TokenInvalidException;
import com.youlai.boot.framework.security.model.AuthenticationToken;
import com.youlai.boot.framework.security.model.SecurityUserDetails;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Redis Token 管理器。
 * <p>
 * 基于 Redis 的有状态 Token 认证，支持 Access + Refresh 双令牌、
 * 单/多设备登录控制、用户级会话失效。
 * <p>
 * 直接存取 {@link SecurityUserDetails}（password 置 null），无需 UserSession 中间层。
 *
 * @author Ray.Hao
 * @since 2024/11/15
 */
@ConditionalOnProperty(value = "security.session.type", havingValue = "redis-token")
@Service
public class RedisTokenManager implements TokenManager {

    private final SecurityProperties securityProperties;
    private final RedisTemplate<String, Object> redisTemplate;
    private final JsonMapper jsonMapper;

    public RedisTokenManager(SecurityProperties securityProperties,
                             RedisTemplate<String, Object> redisTemplate,
                             JsonMapper jsonMapper) {
        this.securityProperties = securityProperties;
        this.redisTemplate = redisTemplate;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public AuthenticationToken generateToken(Authentication authentication) {
        SecurityUserDetails user = (SecurityUserDetails) authentication.getPrincipal();
        String accessToken = IdUtil.fastSimpleUUID();
        String refreshToken = IdUtil.fastSimpleUUID();

        // 构建会话快照（不存密码）
        SecurityUserDetails sessionUser = buildSessionUser(user);

        storeTokensInRedis(accessToken, refreshToken, sessionUser);
        handleSingleDeviceLogin(user.getUserId(), accessToken);

        return AuthenticationToken.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(securityProperties.getSession().getAccessTokenTimeToLive())
                .build();
    }

    @Override
    public Authentication parseToken(String token) {
        Object raw = redisTemplate.opsForValue().get(formatTokenKey(token));
        if (raw == null) return null;
        SecurityUserDetails userDetails = jsonMapper.convertValue(raw, SecurityUserDetails.class);
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    @Override
    public boolean validateToken(String token) {
        return redisTemplate.hasKey(formatTokenKey(token));
    }

    @Override
    public boolean validateRefreshToken(String refreshToken) {
        return redisTemplate.hasKey(formatRefreshTokenKey(refreshToken));
    }

    @Override
    public AuthenticationToken refreshToken(String refreshToken) {
        Object raw = redisTemplate.opsForValue()
                .get(StrUtil.format(RedisConstants.Auth.REFRESH_TOKEN_USER, refreshToken));
        if (raw == null) {
            throw new TokenInvalidException(ResultCode.REFRESH_TOKEN_INVALID);
        }
        SecurityUserDetails sessionUser = jsonMapper.convertValue(raw, SecurityUserDetails.class);

        Object oldAccessTokenValue = redisTemplate.opsForValue()
                .get(StrUtil.format(RedisConstants.Auth.USER_ACCESS_TOKEN, sessionUser.getUserId()));
        Optional.of(oldAccessTokenValue)
                .map(String.class::cast)
                .ifPresent(oldAccessToken -> redisTemplate.delete(formatTokenKey(oldAccessToken)));

        String newAccessToken = IdUtil.fastSimpleUUID();
        storeAccessToken(newAccessToken, sessionUser);

        int accessTtl = securityProperties.getSession().getAccessTokenTimeToLive();
        return AuthenticationToken.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .expiresIn(accessTtl)
                .build();
    }

    @Override
    public void invalidateToken(String token) {
        String cleanToken = cleanBearerPrefix(token);
        redisTemplate.delete(formatTokenKey(cleanToken));
    }

    @Override
    public void invalidateUserSessions(Long userId) {
        if (userId == null) {
            return;
        }
        String userAccessKey = StrUtil.format(RedisConstants.Auth.USER_ACCESS_TOKEN, userId);
        Object accessTokenValue = redisTemplate.opsForValue().get(userAccessKey);
        if (accessTokenValue instanceof String accessToken) {
            redisTemplate.delete(formatTokenKey(accessToken));
        }
        redisTemplate.delete(userAccessKey);

        String userRefreshKey = StrUtil.format(RedisConstants.Auth.USER_REFRESH_TOKEN, userId);
        Object refreshTokenValue = redisTemplate.opsForValue().get(userRefreshKey);
        if (refreshTokenValue instanceof String refreshToken) {
            redisTemplate.delete(StrUtil.format(RedisConstants.Auth.REFRESH_TOKEN_USER, refreshToken));
        }
        redisTemplate.delete(userRefreshKey);
    }

    // ======================== private ========================

    private SecurityUserDetails buildSessionUser(SecurityUserDetails user) {
        SecurityUserDetails sessionUser = new SecurityUserDetails();
        sessionUser.setUserId(user.getUserId());
        sessionUser.setUsername(user.getUsername());
        sessionUser.setDeptId(user.getDeptId());
        sessionUser.setDataScopes(user.getDataScopes());
        sessionUser.setRoles(user.getRoles());
        sessionUser.setEnabled(user.isEnabled());
        sessionUser.setPassword(null);
        return sessionUser;
    }

    private void storeTokensInRedis(String accessToken, String refreshToken, SecurityUserDetails sessionUser) {
        setRedisValue(formatTokenKey(accessToken), sessionUser,
                securityProperties.getSession().getAccessTokenTimeToLive());
        String refreshTokenKey = StrUtil.format(RedisConstants.Auth.REFRESH_TOKEN_USER, refreshToken);
        setRedisValue(refreshTokenKey, sessionUser,
                securityProperties.getSession().getRefreshTokenTimeToLive());
        setRedisValue(StrUtil.format(RedisConstants.Auth.USER_REFRESH_TOKEN, sessionUser.getUserId()),
                refreshToken, securityProperties.getSession().getRefreshTokenTimeToLive());
    }

    private void handleSingleDeviceLogin(Long userId, String accessToken) {
        Boolean allowMultiLogin = securityProperties.getSession().getRedisToken().getAllowMultiLogin();
        String userAccessKey = StrUtil.format(RedisConstants.Auth.USER_ACCESS_TOKEN, userId);
        if (!allowMultiLogin) {
            Object oldAccessTokenValue = redisTemplate.opsForValue().get(userAccessKey);
            if (oldAccessTokenValue instanceof String oldAccessToken) {
                redisTemplate.delete(formatTokenKey(oldAccessToken));
            }
        }
        setRedisValue(userAccessKey, accessToken, securityProperties.getSession().getAccessTokenTimeToLive());
    }

    private void storeAccessToken(String newAccessToken, SecurityUserDetails sessionUser) {
        setRedisValue(StrUtil.format(RedisConstants.Auth.ACCESS_TOKEN_USER, newAccessToken), sessionUser,
                securityProperties.getSession().getAccessTokenTimeToLive());
        String userAccessKey = StrUtil.format(RedisConstants.Auth.USER_ACCESS_TOKEN, sessionUser.getUserId());
        setRedisValue(userAccessKey, newAccessToken, securityProperties.getSession().getAccessTokenTimeToLive());
    }

    private String formatTokenKey(String token) {
        return StrUtil.format(RedisConstants.Auth.ACCESS_TOKEN_USER, token);
    }

    private String formatRefreshTokenKey(String refreshToken) {
        return StrUtil.format(RedisConstants.Auth.REFRESH_TOKEN_USER, refreshToken);
    }

    private void setRedisValue(String key, Object value, int ttl) {
        if (ttl != -1) {
            redisTemplate.opsForValue().set(key, value, ttl, TimeUnit.SECONDS);
        } else {
            redisTemplate.opsForValue().set(key, value);
        }
    }

    private String cleanBearerPrefix(String token) {
        if (token.startsWith(SecurityConstants.BEARER_TOKEN_PREFIX)) {
            return token.substring(SecurityConstants.BEARER_TOKEN_PREFIX.length()).trim();
        }
        return token.trim();
    }
}
