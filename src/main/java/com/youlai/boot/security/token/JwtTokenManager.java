package com.youlai.boot.security.token;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTPayload;
import cn.hutool.jwt.JWTUtil;
import com.youlai.boot.common.constant.JwtClaimConstants;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.constant.SecurityConstants;
import com.youlai.boot.core.exception.BusinessException;
import com.youlai.boot.core.web.ResultCode;
import com.youlai.boot.config.property.SecurityProperties;
import com.youlai.boot.security.model.AuthenticationToken;
import org.apache.commons.lang3.StringUtils;
import com.youlai.boot.security.model.SysUserDetails;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * JWT Token 管理器
 * <p>
 * 用于生成、解析、校验、刷新 JWT Token
 *
 * @author Ray.Hao
 * @since 2024/11/15
 */
@ConditionalOnProperty(value = "security.session.type", havingValue = "jwt")
@Service
public class JwtTokenManager implements TokenManager {

    private final SecurityProperties securityProperties;
    private final RedisTemplate<String, Object> redisTemplate;
    private final byte[] secretKey;

    public JwtTokenManager(SecurityProperties securityProperties, RedisTemplate<String, Object> redisTemplate) {
        this.securityProperties = securityProperties;
        this.redisTemplate = redisTemplate;
        this.secretKey = securityProperties.getSession().getJwt().getSecretKey().getBytes();
    }

    /**
     * 生成令牌
     *
     * @param authentication 认证信息
     * @return 令牌响应对象
     */
    @Override
    public AuthenticationToken generateToken(Authentication authentication) {
        int accessTokenTimeToLive = securityProperties.getSession().getAccessTokenTimeToLive();
        int refreshTokenTimeToLive = securityProperties.getSession().getRefreshTokenTimeToLive();

        String accessToken = generateToken(authentication, accessTokenTimeToLive);
        String refreshToken = generateToken(authentication, refreshTokenTimeToLive, true);

        return AuthenticationToken.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenTimeToLive)
                .build();
    }

    /**
     * 解析令牌
     *
     * @param token JWT Token
     * @return Authentication 对象
     */
    @Override
    public Authentication parseToken(String token) {

        JWT jwt = JWTUtil.parseToken(token);
        JSONObject payloads = jwt.getPayloads();
        SysUserDetails userDetails = new SysUserDetails();
        userDetails.setUserId(payloads.getLong(JwtClaimConstants.USER_ID)); // 用户ID
        userDetails.setDeptId(payloads.getLong(JwtClaimConstants.DEPT_ID)); // 部门ID
        userDetails.setDataScope(payloads.getInt(JwtClaimConstants.DATA_SCOPE)); // 数据权限范围

        userDetails.setUsername(payloads.getStr(JWTPayload.SUBJECT)); // 用户名
        // 角色集合
        Set<SimpleGrantedAuthority> authorities = payloads.getJSONArray(JwtClaimConstants.AUTHORITIES)
                .stream()
                .map(authority -> new SimpleGrantedAuthority(Convert.toStr(authority)))
                .collect(Collectors.toSet());

        return new UsernamePasswordAuthenticationToken(userDetails, "", authorities);
    }

    /**
     * 校验令牌
     *
     * @param token JWT Token
     * @return 是否有效
     */
    @Override
    public boolean validateToken(String token) {
        return validateToken(token, false);
    }

    /**
     * 校验刷新令牌
     *
     * @param refreshToken JWT Token
     * @return 验证结果
     */
    @Override
    public boolean validateRefreshToken(String refreshToken) {
        return validateToken(refreshToken, true);
    }

    /**
     * 校验令牌
     *
     * @param token                JWT Token
     * @param validateRefreshToken 是否校验刷新令牌
     * @return 是否有效
     */
    private boolean validateToken(String token, boolean validateRefreshToken) {
        try {
            JWT jwt = JWTUtil.parseToken(token);
            // 检查 Token 是否有效(验签 + 是否过期)
            boolean isValid = jwt.setKey(secretKey).validate(0);

            if (isValid) {
                JSONObject payloads = jwt.getPayloads();
                // 1. 校验刷新令牌类型（仅在校验刷新令牌场景启用）
                String jti = payloads.getStr(JWTPayload.JWT_ID);
                if (validateRefreshToken) {
                    //刷新token需要校验token类别
                    boolean isRefreshToken = payloads.getBool(JwtClaimConstants.TOKEN_TYPE);
                    if (!isRefreshToken) {
                        return false;
                    }
                }
                // 2. 校验安全版本号（用于按用户维度失效历史 Token）
                Long userId = payloads.getLong(JwtClaimConstants.USER_ID);
                if (userId != null) {
                    // 老版本 Token 可能没有 SECURITY_VERSION 声明，视为 0 版本
                    Integer tokenVersionRaw = payloads.getInt(JwtClaimConstants.SECURITY_VERSION);
                    int tokenVersion = tokenVersionRaw != null ? tokenVersionRaw : 0;

                    String versionKey = StrUtil.format(RedisConstants.Auth.USER_SECURITY_VERSION, userId);
                    Integer currentVersionRaw = (Integer) redisTemplate.opsForValue().get(versionKey);
                    int currentVersion = currentVersionRaw != null ? currentVersionRaw : 0;

                    // 如果当前版本号比 Token 携带的版本号新，则认为该 Token 已失效
                    if (tokenVersion < currentVersion) {
                        return false;
                    }
                }

                // 3. 判断是否在黑名单中，如果在，则返回 false 标识Token无效
                if (Boolean.TRUE.equals(redisTemplate.hasKey(StrUtil.format(RedisConstants.Auth.BLACKLIST_TOKEN, jti)))) {
                    return false;
                }
            }
            return isValid;
        } catch (Exception gitignore) {
            // token 验证
        }
        return false;
    }

    /**
     * 将令牌加入黑名单
     *
     * @param token JWT Token
     */
    @Override
    public void invalidateToken(String token) {
        if (StringUtils.isBlank(token)) {
            return;
        }

        if (token.startsWith(SecurityConstants.BEARER_TOKEN_PREFIX)) {
            token = token.substring(SecurityConstants.BEARER_TOKEN_PREFIX.length());
        }
        JWT jwt = JWTUtil.parseToken(token);
        JSONObject payloads = jwt.getPayloads();
        Integer expirationAt = payloads.getInt(JWTPayload.EXPIRES_AT);
        // 黑名单Token Key
        String blacklistTokenKey = StrUtil.format(RedisConstants.Auth.BLACKLIST_TOKEN, payloads.getStr(JWTPayload.JWT_ID));

        if (expirationAt != null) {
            int currentTimeSeconds = Convert.toInt(System.currentTimeMillis() / 1000);
            if (expirationAt < currentTimeSeconds) {
                // Token已过期，直接返回
                return;
            }
            // 计算Token剩余时间，将其加入黑名单（值使用简单布尔标记即可）
            int expirationIn = expirationAt - currentTimeSeconds;
            redisTemplate.opsForValue().set(blacklistTokenKey, Boolean.TRUE, expirationIn, TimeUnit.SECONDS);
        } else {
            // 永不过期的Token永久加入黑名单
            redisTemplate.opsForValue().set(blacklistTokenKey, Boolean.TRUE);
        }
        ;
    }

    /**
     * 失效指定用户的所有会话
     * <p>
     * 通过提升用户的安全版本号，使携带旧版本号的 Token 在后续校验时全部失效
     */
    @Override
    public void invalidateUserSessions(Long userId) {
        if (userId == null) {
            return;
        }

        String versionKey = StrUtil.format(RedisConstants.Auth.USER_SECURITY_VERSION, userId);
        // 递增版本号
        redisTemplate.opsForValue().increment(versionKey);

    }

    /**
     * 刷新令牌
     *
     * @param refreshToken 刷新令牌
     * @return 令牌响应对象
     */
    @Override
    public AuthenticationToken refreshToken(String refreshToken) {
        boolean isValid = validateRefreshToken(refreshToken);
        if (!isValid) {
            throw new BusinessException(ResultCode.REFRESH_TOKEN_INVALID);
        }
        Authentication authentication = parseToken(refreshToken);
        int accessTokenExpiration = securityProperties.getSession().getAccessTokenTimeToLive();
        String newAccessToken = generateToken(authentication, accessTokenExpiration);
        return AuthenticationToken.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration)
                .build();
    }

    /**
     * 生成 JWT Token
     *
     * @param authentication 认证信息
     * @param ttl            过期时间
     * @return JWT Token
     */
    private String generateToken(Authentication authentication, int ttl) {
        return generateToken(authentication, ttl, false);
    }


    /**
     * 生成 JWT Token
     *
     * @param authentication 认证信息
     * @param ttl            过期时间
     * @param isRefreshToken 类型是否为刷新token
     * @return JWT Token
     */
    private String generateToken(Authentication authentication, int ttl, boolean isRefreshToken) {
        SysUserDetails userDetails = (SysUserDetails) authentication.getPrincipal();
        Map<String, Object> payload = new HashMap<>();
        payload.put(JwtClaimConstants.USER_ID, userDetails.getUserId()); // 用户ID
        payload.put(JwtClaimConstants.DEPT_ID, userDetails.getDeptId()); // 部门ID
        payload.put(JwtClaimConstants.DATA_SCOPE, userDetails.getDataScope()); // 数据权限范围

        // claims 中添加角色信息
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        payload.put(JwtClaimConstants.AUTHORITIES, roles);

        Date now = new Date();
        payload.put(JWTPayload.ISSUED_AT, now);
        payload.put(JwtClaimConstants.TOKEN_TYPE, false);
        if (isRefreshToken) {
            payload.put(JwtClaimConstants.TOKEN_TYPE, true);
        }

        // 设置安全版本号：不存在时默认为 0
        String versionKey = StrUtil.format(RedisConstants.Auth.USER_SECURITY_VERSION, userDetails.getUserId());
        Integer currentVersion = (Integer) redisTemplate.opsForValue().get(versionKey);
        int securityVersion = currentVersion != null ? currentVersion : 0;
        payload.put(JwtClaimConstants.SECURITY_VERSION, securityVersion);

        // 设置过期时间 -1 表示永不过期
        if (ttl != -1) {
            Date expiresAt = DateUtil.offsetSecond(now, ttl);
            payload.put(JWTPayload.EXPIRES_AT, expiresAt);
        }
        payload.put(JWTPayload.SUBJECT, authentication.getName());
        payload.put(JWTPayload.JWT_ID, IdUtil.simpleUUID());

        return JWTUtil.createToken(payload, secretKey);
    }

}
