package com.youlai.boot.security.token;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.core.exception.BusinessException;
import com.youlai.boot.core.web.ResultCode;
import com.youlai.boot.config.property.SecurityProperties;
import com.youlai.boot.security.model.AuthenticationToken;
import com.youlai.boot.security.model.OnlineUser;
import com.youlai.boot.security.model.SysUserDetails;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Redis Token 管理器
 * <p>
 * 用于生成、解析、校验、刷新 Redis Token
 *
 * @author Ray.Hao
 * @since 2024/11/15
 */
@ConditionalOnProperty(value = "security.session.type", havingValue = "redis-token")
@Service
public class RedisTokenManager implements TokenManager {

    private final SecurityProperties securityProperties;
    private final RedisTemplate<String, Object> redisTemplate;

    public RedisTokenManager(SecurityProperties securityProperties, RedisTemplate<String, Object> redisTemplate) {
        this.securityProperties = securityProperties;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 生成 Token
     *
     * @param authentication 用户认证信息
     * @return 生成的 AuthenticationToken 对象
     */
    @Override
    public AuthenticationToken generateToken(Authentication authentication) {
        SysUserDetails user = (SysUserDetails) authentication.getPrincipal();
        String accessToken = IdUtil.fastSimpleUUID();
        String refreshToken = IdUtil.fastSimpleUUID();

        // 构建用户在线信息
        OnlineUser onlineUser = new OnlineUser(
                user.getUserId(),
                user.getUsername(),
                user.getDeptId(),
                user.getDataScope(),
                user.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toSet())
        );

        // 存储访问令牌、刷新令牌和刷新令牌映射
        storeTokensInRedis(accessToken, refreshToken, onlineUser);

        // 单设备登录控制
        handleSingleDeviceLogin(user.getUserId(), accessToken);

        return AuthenticationToken.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(securityProperties.getSession().getAccessTokenTimeToLive())
                .build();
    }

    /**
     * 根据 token 解析用户信息
     *
     * @param token Redis Token
     * @return 构建的 Authentication 对象
     */
    @Override
    public Authentication parseToken(String token) {
        OnlineUser onlineUser = (OnlineUser) redisTemplate.opsForValue().get(formatTokenKey(token));
        if (onlineUser == null) return null;

        // 构建用户权限集合
        Set<SimpleGrantedAuthority> authorities = null;

        Set<String> roles = onlineUser.getRoles();
        if (CollectionUtil.isNotEmpty(roles)) {
            authorities = roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toSet());
        }

        // 构建用户详情对象
        SysUserDetails userDetails = buildUserDetails(onlineUser, authorities);
        return new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
    }

    /**
     * 校验 Token 是否有效
     *
     * @param token 访问令牌
     * @return 是否有效
     */
    @Override
    public boolean validateToken(String token) {
        return redisTemplate.hasKey(formatTokenKey(token));
    }

    /**
     * 校验 RefreshToken 是否有效
     *
     * @param refreshToken 访问令牌
     * @return 是否有效
     */
    @Override
    public boolean validateRefreshToken(String refreshToken) {
        return redisTemplate.hasKey(formatRefreshTokenKey(refreshToken));
    }

    /**
     * 刷新令牌
     *
     * @param refreshToken 刷新令牌
     * @return 新生成的 AuthenticationToken 对象
     */
    @Override
    public AuthenticationToken refreshToken(String refreshToken) {
        OnlineUser onlineUser = (OnlineUser) redisTemplate.opsForValue()
                .get(StrUtil.format(RedisConstants.Auth.REFRESH_TOKEN_USER, refreshToken));
        if (onlineUser == null) {
            throw new BusinessException(ResultCode.REFRESH_TOKEN_INVALID);
        }
        Object oldAccessTokenValue = redisTemplate.opsForValue().get(StrUtil.format(RedisConstants.Auth.USER_ACCESS_TOKEN, onlineUser.getUserId()));
        // 删除旧的访问令牌记录
        Optional.of(oldAccessTokenValue)
                .map(String.class::cast)
                .ifPresent(oldAccessToken -> redisTemplate.delete(formatTokenKey(oldAccessToken)));

        // 生成新访问令牌并存储
        String newAccessToken = IdUtil.fastSimpleUUID();
        storeAccessToken(newAccessToken, onlineUser);

        int accessTtl = securityProperties.getSession().getAccessTokenTimeToLive();
        return AuthenticationToken.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .expiresIn(accessTtl)
                .build();
    }

    /**
     * 使访问令牌失效
     *
     * @param token 访问令牌
     */
    @Override
    public void invalidateToken(String token) {
        OnlineUser onlineUser = (OnlineUser) redisTemplate.opsForValue().get(formatTokenKey(token));
        if (onlineUser != null) {
            Long userId = onlineUser.getUserId();
            invalidateUserSessions(userId);
        }
    }

    /**
     * 使指定用户的所有会话失效
     *
     * @param userId 用户ID
     */
    @Override
    public void invalidateUserSessions(Long userId) {
        if (userId == null) {
            return;
        }

        // 1. 删除访问令牌相关
        String userAccessKey = StrUtil.format(RedisConstants.Auth.USER_ACCESS_TOKEN, userId);
        Object accessTokenValue = redisTemplate.opsForValue().get(userAccessKey);
        Optional.of(accessTokenValue)
                .map(String.class::cast)
                .ifPresent(accessToken -> redisTemplate.delete(formatTokenKey(accessToken)));
        // 无论是否存在访问令牌映射，都尝试删除 userAccessKey
        redisTemplate.delete(userAccessKey);

        // 2. 删除刷新令牌相关
        String userRefreshKey = StrUtil.format(RedisConstants.Auth.USER_REFRESH_TOKEN, userId);
        Object refreshTokenValue = redisTemplate.opsForValue().get(userRefreshKey);
        Optional.of(refreshTokenValue)
                .map(String.class::cast)
                .ifPresent(refreshToken ->
                        redisTemplate.delete(StrUtil.format(RedisConstants.Auth.REFRESH_TOKEN_USER, refreshToken))
                );
        // 同样清理 userRefreshKey 本身
        redisTemplate.delete(userRefreshKey);
    }

    /**
     * 将访问令牌和刷新令牌存储至 Redis
     *
     * @param accessToken  访问令牌
     * @param refreshToken 刷新令牌
     * @param onlineUser   在线用户信息
     */
    private void storeTokensInRedis(String accessToken, String refreshToken, OnlineUser onlineUser) {
        // 访问令牌 -> 用户信息
        setRedisValue(formatTokenKey(accessToken), onlineUser, securityProperties.getSession().getAccessTokenTimeToLive());

        // 刷新令牌 -> 用户信息
        String refreshTokenKey = StrUtil.format(RedisConstants.Auth.REFRESH_TOKEN_USER, refreshToken);
        setRedisValue(refreshTokenKey, onlineUser, securityProperties.getSession().getRefreshTokenTimeToLive());

        // 用户ID -> 刷新令牌
        setRedisValue(StrUtil.format(RedisConstants.Auth.USER_REFRESH_TOKEN, onlineUser.getUserId()),
                refreshToken,
                securityProperties.getSession().getRefreshTokenTimeToLive());
    }

    /**
     * 处理单设备登录控制
     *
     * @param userId      用户ID
     * @param accessToken 新生成的访问令牌
     */
    private void handleSingleDeviceLogin(Long userId, String accessToken) {
        Boolean allowMultiLogin = securityProperties.getSession().getRedisToken().getAllowMultiLogin();
        String userAccessKey = StrUtil.format(RedisConstants.Auth.USER_ACCESS_TOKEN, userId);
        // 单设备登录控制，删除旧的访问令牌
        if (!allowMultiLogin) {
            Object oldAccessTokenValue = redisTemplate.opsForValue().get(userAccessKey);
            Optional.of(oldAccessTokenValue)
                    .map(String.class::cast)
                    .ifPresent(oldAccessToken -> redisTemplate.delete(formatTokenKey(oldAccessToken)));
        }
        // 存储访问令牌映射（用户ID -> 访问令牌），用于单设备登录控制删除旧的访问令牌和刷新令牌时删除旧令牌
        setRedisValue(userAccessKey, accessToken, securityProperties.getSession().getAccessTokenTimeToLive());
    }

    /**
     * 存储新的访问令牌
     *
     * @param newAccessToken 新访问令牌
     * @param onlineUser     在线用户信息
     */
    private void storeAccessToken(String newAccessToken, OnlineUser onlineUser) {
        setRedisValue(StrUtil.format(RedisConstants.Auth.ACCESS_TOKEN_USER, newAccessToken), onlineUser, securityProperties.getSession().getAccessTokenTimeToLive());
        String userAccessKey = StrUtil.format(RedisConstants.Auth.USER_ACCESS_TOKEN, onlineUser.getUserId());
        setRedisValue(userAccessKey, newAccessToken, securityProperties.getSession().getAccessTokenTimeToLive());
    }

    /**
     * 构建用户详情对象
     *
     * @param onlineUser  在线用户信息
     * @param authorities 权限集合
     * @return SysUserDetails 用户详情
     */
    private SysUserDetails buildUserDetails(OnlineUser onlineUser, Set<SimpleGrantedAuthority> authorities) {
        SysUserDetails userDetails = new SysUserDetails();
        userDetails.setUserId(onlineUser.getUserId());
        userDetails.setUsername(onlineUser.getUsername());
        userDetails.setDeptId(onlineUser.getDeptId());
        userDetails.setDataScope(onlineUser.getDataScope());
        userDetails.setAuthorities(authorities);
        return userDetails;
    }

    /**
     * 格式化访问令牌的 Redis 键
     *
     * @param token 访问令牌
     * @return 格式化后的 Redis 键
     */
    private String formatTokenKey(String token) {
        return StrUtil.format(RedisConstants.Auth.ACCESS_TOKEN_USER, token);
    }

    /**
     * 格式化刷新令牌的 Redis 键
     *
     * @param refreshToken 访问令牌
     * @return 格式化后的 Redis 键
     */
    private String formatRefreshTokenKey(String refreshToken) {
        return StrUtil.format(RedisConstants.Auth.REFRESH_TOKEN_USER, refreshToken);
    }

    /**
     * 将值存储到 Redis
     *
     * @param key   键
     * @param value 值
     * @param ttl   过期时间（秒），-1表示永不过期
     */
    private void setRedisValue(String key, Object value, int ttl) {
        if (ttl != -1) {
            redisTemplate.opsForValue().set(key, value, ttl, TimeUnit.SECONDS);
        } else {
            redisTemplate.opsForValue().set(key, value); // ttl=-1时永不过期
        }
    }
}
