package com.youlai.boot.security.token;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
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
import com.youlai.boot.security.model.RoleDataScope;
import org.apache.commons.lang3.StringUtils;
import com.youlai.boot.security.model.SysUserDetails;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * JWT Token 管理器
 * <p>
 * 实现基于JWT的无状态认证，支持：
 * <ul>
 *   <li>Access Token + Refresh Token 双令牌机制</li>
 *   <li>Token 撤销（jti黑名单）</li>
 *   <li>用户级会话失效（tokenValidAfter）</li>
 *   <li>多角色数据权限存储</li>
 * </ul>
 *
 * @author Ray.Hao
 * @since 2024/11/15
 */
@ConditionalOnProperty(value = "security.session.type", havingValue = "jwt")
@Service
public class JwtTokenManager implements TokenManager {

    /** tokenValidAfter 默认过期时间（7天），避免Redis内存泄漏 */
    private static final long TOKEN_VALID_AFTER_TTL_SECONDS = 7 * 24 * 60 * 60;

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

        // 解析数据权限列表
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
     * <p>
     * 校验流程（按顺序执行）：
     * <ol>
     *   <li>签名验证 + 过期时间检查</li>
     *   <li>刷新令牌类型校验（仅刷新场景）</li>
     *   <li>tokenValidAfter 校验（用户级会话失效）</li>
     *   <li>jti 黑名单校验（单Token撤销）</li>
     * </ol>
     *
     * @param token                JWT Token
     * @param validateRefreshToken 是否校验刷新令牌类型
     * @return 是否有效
     */
    private boolean validateToken(String token, boolean validateRefreshToken) {
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
            // 2. 校验 tokenValidAfter（用于按用户维度失效历史 Token）
            //    场景示例：用户修改密码、被管理员强制下线、手动“踢所有端”后，更新 tokenValidAfter，早于该时间签发的 Token 全部失效
            Long userId = payloads.getLong(JwtClaimConstants.USER_ID);
            if (userId != null) {
                Object issuedAtObj = payloads.get(JWTPayload.ISSUED_AT);
                long issuedAtSeconds = 0;
                if (issuedAtObj instanceof Date issuedAtDate) {
                    issuedAtSeconds = issuedAtDate.getTime() / 1000;
                }

                String validAfterKey = StrUtil.format(RedisConstants.Auth.USER_TOKEN_VALID_AFTER, userId);
                Object validAfterObj = redisTemplate.opsForValue().get(validAfterKey);
                long validAfterSeconds = validAfterObj != null ? Convert.toLong(validAfterObj) : 0;

                if (issuedAtSeconds < validAfterSeconds) {
                    return false;
                }
            }

            // 3. 判断 Token 是否已被撤销（单端退出/会话注销）
            //    场景示例：单点退出登录、后台手动注销某个会话、封禁账号后立即阻断当前 Token 等
            if (isTokenRevoked(jti)) {
                return false;
            }
        }
        return isValid;
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
        String jti = payloads.getStr(JWTPayload.JWT_ID);
        Integer expirationAt = payloads.getInt(JWTPayload.EXPIRES_AT);
        revokeTokenByJti(jti, expirationAt);
    }

    /**
     * 检查Token是否已被撤销
     *
     * @param jti Token唯一标识
     * @return true-已撤销，false-未撤销
     */
    private boolean isTokenRevoked(String jti) {
        if (StringUtils.isBlank(jti)) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(StrUtil.format(RedisConstants.Auth.REVOKED_JTI, jti)));
    }

    /**
     * 将Token加入撤销黑名单
     * <p>
     * 黑名单有效期与Token剩余有效期一致，避免永久存储
     *
     * @param jti          Token唯一标识
     * @param expirationAt Token过期时间戳
     */
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

    /**
     * 失效指定用户的所有会话
     * <p>
     * 通过更新用户 tokenValidAfter 时间戳，使早于该时间签发的 Token 全部失效。
     * <p>
     * 适用场景：
     * <ul>
     *   <li>用户修改密码</li>
     *   <li>管理员强制下线用户</li>
     *   <li>用户主动踢出所有设备</li>
     * </ul>
     *
     * @param userId 用户ID
     */
    @Override
    public void invalidateUserSessions(Long userId) {
        if (userId == null) {
            return;
        }

        String validAfterKey = StrUtil.format(RedisConstants.Auth.USER_TOKEN_VALID_AFTER, userId);
        long nowSeconds = System.currentTimeMillis() / 1000;
        // 设置过期时间，避免Redis内存泄漏
        redisTemplate.opsForValue().set(validAfterKey, nowSeconds, TOKEN_VALID_AFTER_TTL_SECONDS, TimeUnit.SECONDS);
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
     * @param ttl            过期时间（秒），-1表示永不过期
     * @return JWT Token字符串
     */
    private String generateToken(Authentication authentication, int ttl) {
        return generateToken(authentication, ttl, false);
    }


    /**
     * 生成 JWT Token
     * <p>
     * Payload包含：
     * <ul>
     *   <li>userId - 用户ID</li>
     *   <li>deptId - 部门ID</li>
     *   <li>dataScopes - 数据权限列表</li>
     *   <li>authorities - 角色权限集合</li>
     *   <li>tokenType - 是否为刷新令牌</li>
     *   <li>iat/exp - 签发/过期时间</li>
     *   <li>jti - Token唯一标识（用于撤销）</li>
     * </ul>
     *
     * @param authentication  认证信息
     * @param ttl             过期时间（秒）
     * @param isRefreshToken  是否为刷新令牌
     * @return JWT Token字符串
     */
    private String generateToken(Authentication authentication, int ttl, boolean isRefreshToken) {
        SysUserDetails userDetails = (SysUserDetails) authentication.getPrincipal();
        Map<String, Object> payload = new HashMap<>();
        payload.put(JwtClaimConstants.USER_ID, userDetails.getUserId()); // 用户ID
        payload.put(JwtClaimConstants.DEPT_ID, userDetails.getDeptId()); // 部门ID

        // 存储数据权限列表
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
