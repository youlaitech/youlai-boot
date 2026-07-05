package com.youlai.boot.framework.security.token;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTPayload;
import cn.hutool.jwt.JWTUtil;
import com.youlai.boot.common.constant.JwtClaimConstants;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.constant.SecurityConstants;
import com.youlai.boot.common.result.ResultCode;
import com.youlai.boot.framework.security.config.SecurityProperties;
import com.youlai.boot.framework.security.exception.TokenInvalidException;
import com.youlai.boot.framework.security.model.AuthenticationToken;
import com.youlai.boot.framework.security.model.RoleDataScope;
import com.youlai.boot.framework.security.model.SecurityUserDetails;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * JWT Token 管理器。
 * <p>
 * 基于 JWT 的无状态认证，支持 Access + Refresh 双令牌、Token 撤销（jti 黑名单）、
 * 用户级会话失效（tokenVersion）、多角色数据权限存储。
 * <p>
 * JWT claims 中存储角色编码（不带 ROLE_ 前缀），解析后由 {@link SecurityUserDetails#getAuthorities()} 运行时补前缀。
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

    @Override
    public Authentication parseToken(String token) {
        JWT jwt = JWTUtil.parseToken(token);
        JSONObject payloads = jwt.getPayloads();
        SecurityUserDetails userDetails = new SecurityUserDetails();
        userDetails.setUserId(payloads.getLong(JwtClaimConstants.USER_ID));
        userDetails.setDeptId(payloads.getLong(JwtClaimConstants.DEPT_ID));

        // 数据权限
        JSONArray dataScopesArray = payloads.getJSONArray(JwtClaimConstants.DATA_SCOPES);
        if (dataScopesArray != null && !dataScopesArray.isEmpty()) {
            List<RoleDataScope> dataScopes = dataScopesArray.stream()
                    .map(obj -> {
                        JSONObject item = (JSONObject) obj;
                        String roleCode = item.getStr("roleCode");
                        Integer dataScope = item.getInt("dataScope");
                        JSONArray deptIdsArray = item.getJSONArray("customDeptIds");
                        List<Long> customDeptIds = null;
                        if (deptIdsArray != null) {
                            customDeptIds = deptIdsArray.toList(Long.class);
                        }
                        return new RoleDataScope(roleCode, dataScope, customDeptIds);
                    })
                    .collect(Collectors.toList());
            userDetails.setDataScopes(dataScopes);
        }

        userDetails.setUsername(payloads.getStr(JWTPayload.SUBJECT));

        // 角色编码（不带 ROLE_ 前缀）
        JSONArray rolesArray = payloads.getJSONArray(JwtClaimConstants.ROLES);
        if (rolesArray != null && !rolesArray.isEmpty()) {
            Set<String> roles = rolesArray.stream()
                    .map(Convert::toStr)
                    .collect(Collectors.toSet());
            userDetails.setRoles(roles);
        }

        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    @Override
    public boolean validateToken(String token) {
        return validateToken(token, false);
    }

    @Override
    public boolean validateRefreshToken(String refreshToken) {
        return validateToken(refreshToken, true);
    }

    private boolean validateToken(String token, boolean validateRefreshToken) {
        JWT jwt = JWTUtil.parseToken(token);
        boolean isValid = jwt.setKey(secretKey).validate(0);

        if (isValid) {
            JSONObject payloads = jwt.getPayloads();
            // 刷新令牌类型校验
            String jti = payloads.getStr(JWTPayload.JWT_ID);
            if (validateRefreshToken) {
                boolean isRefreshToken = payloads.getBool(JwtClaimConstants.TOKEN_TYPE);
                if (!isRefreshToken) {
                    return false;
                }
            }

            // tokenVersion 校验（用户维度 Token 失效）
            Long userId = payloads.getLong(JwtClaimConstants.USER_ID);
            if (userId != null) {
                Integer tokenVersion = payloads.getInt(JwtClaimConstants.TOKEN_VERSION);

                String versionKey = StrUtil.format(RedisConstants.Auth.USER_TOKEN_VERSION, userId);
                Object currentVersionObj = redisTemplate.opsForValue().get(versionKey);
                int currentVersion = currentVersionObj != null ? Convert.toInt(currentVersionObj) : 0;

                if (tokenVersion == null || tokenVersion < currentVersion) {
                    return false;
                }
            }

            // jti 黑名单校验
            if (isTokenRevoked(jti)) {
                return false;
            }
        }
        return isValid;
    }

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
        String jti = payloads.getStr(JWTPayload.JWT_ID);
        Integer expirationAt = payloads.getInt(JWTPayload.EXPIRES_AT);
        revokeTokenByJti(jti, expirationAt);
    }

    @Override
    public void invalidateUserSessions(Long userId) {
        if (userId == null) {
            return;
        }
        String versionKey = StrUtil.format(RedisConstants.Auth.USER_TOKEN_VERSION, userId);
        redisTemplate.opsForValue().increment(versionKey);
    }

    @Override
    public AuthenticationToken refreshToken(String refreshToken) {
        boolean isValid = validateRefreshToken(refreshToken);
        if (!isValid) {
            throw new TokenInvalidException(ResultCode.REFRESH_TOKEN_INVALID);
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

    // ======================== private ========================

    private String generateToken(Authentication authentication, int ttl) {
        return generateToken(authentication, ttl, false);
    }

    private String generateToken(Authentication authentication, int ttl, boolean isRefreshToken) {
        SecurityUserDetails userDetails = (SecurityUserDetails) authentication.getPrincipal();
        Map<String, Object> payload = new HashMap<>();
        payload.put(JwtClaimConstants.USER_ID, userDetails.getUserId());
        payload.put(JwtClaimConstants.DEPT_ID, userDetails.getDeptId());

        // 数据权限
        List<RoleDataScope> dataScopes = userDetails.getDataScopes();
        if (dataScopes != null && !dataScopes.isEmpty()) {
            List<Map<String, Object>> scopesList = dataScopes.stream()
                    .map(scope -> {
                        Map<String, Object> scopeMap = new HashMap<>();
                        scopeMap.put("roleCode", scope.getRoleCode());
                        scopeMap.put("dataScope", scope.getDataScope());
                        scopeMap.put("customDeptIds", scope.getCustomDeptIds());
                        return scopeMap;
                    })
                    .collect(Collectors.toList());
            payload.put(JwtClaimConstants.DATA_SCOPES, scopesList);
        }

        // 角色编码（不带 ROLE_ 前缀）
        Set<String> roles = userDetails.getRoles();
        if (roles != null && !roles.isEmpty()) {
            payload.put(JwtClaimConstants.ROLES, roles);
        }

        Long userId = userDetails.getUserId();
        int tokenVersion = 0;
        if (userId != null) {
            String versionKey = StrUtil.format(RedisConstants.Auth.USER_TOKEN_VERSION, userId);
            Object versionObj = redisTemplate.opsForValue().get(versionKey);
            tokenVersion = versionObj != null ? Convert.toInt(versionObj) : 0;
        }
        payload.put(JwtClaimConstants.TOKEN_VERSION, tokenVersion);

        Date now = new Date();
        payload.put(JWTPayload.ISSUED_AT, now);
        payload.put(JwtClaimConstants.TOKEN_TYPE, isRefreshToken);

        if (ttl != -1) {
            Date expiresAt = DateUtil.offsetSecond(now, ttl);
            payload.put(JWTPayload.EXPIRES_AT, expiresAt);
        }
        payload.put(JWTPayload.SUBJECT, authentication.getName());
        payload.put(JWTPayload.JWT_ID, IdUtil.simpleUUID());

        return JWTUtil.createToken(payload, secretKey);
    }

    private boolean isTokenRevoked(String jti) {
        if (StringUtils.isBlank(jti)) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(StrUtil.format(RedisConstants.Auth.REVOKED_JTI, jti)));
    }

    private void revokeTokenByJti(String jti, Integer expirationAt) {
        if (StringUtils.isBlank(jti)) {
            return;
        }
        String revokedJtiKey = StrUtil.format(RedisConstants.Auth.REVOKED_JTI, jti);
        if (expirationAt != null) {
            int currentTimeSeconds = Convert.toInt(System.currentTimeMillis() / 1000);
            if (expirationAt < currentTimeSeconds) {
                return;
            }
            int expirationIn = expirationAt - currentTimeSeconds;
            redisTemplate.opsForValue().set(revokedJtiKey, Boolean.TRUE, expirationIn, TimeUnit.SECONDS);
        } else {
            redisTemplate.opsForValue().set(revokedJtiKey, Boolean.TRUE);
        }
    }
}
