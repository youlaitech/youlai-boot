package com.youlai.boot.config.property;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * 安全模块配置属性类
 *
 * <p>映射 application.yml 中 security 前缀的安全相关配置</p>
 *
 * @author Ray.Hao
 * @since 2024/4/18
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    /**
     * 会话管理配置
     */
    private SessionConfig session;

    /**
     * 安全白名单路径（完全绕过安全过滤器）
     * <p>示例值：/api/v1/auth/login/**, /ws/**
     */
    @NotEmpty
    private String[] ignoreUrls;

    /**
     * 非安全端点路径（允许匿名访问的API）
     * <p>示例值：/doc.html, /v3/api-docs/**
     */
    @NotEmpty
    private String[] unsecuredUrls;

    /**
     * 会话配置嵌套类
     */
    @Data
    public static class SessionConfig {
        /**
         * 认证策略类型
         * <ul>
         *   <li>jwt - 基于JWT的无状态认证</li>
         *   <li>redis-token - 基于Redis的有状态认证</li>
         * </ul>
         */
        @NotNull(message = "会话类型不能为空")
        @Pattern(regexp = "jwt|redis-token", message = "会话类型只能是 jwt 或 redis-token")
        private String type;

        /**
         * 访问令牌有效期（单位：秒）
         * <p>默认值：3600（1小时）</p>
         * <p>-1 表示永不过期</p>
         */
        @Min(-1)
        private Integer accessTokenTimeToLive = 3600;

        /**
         * 刷新令牌有效期（单位：秒）
         * <p>默认值：604800（7天）</p>
         * <p>-1 表示永不过期</p>
         */
        @Min(-1)
        private Integer refreshTokenTimeToLive = 604800;

        /**
         * JWT 配置项
         */
        private JwtConfig jwt;

        /**
         * Redis令牌配置项
         */
        private RedisTokenConfig redisToken;
    }

    /**
     * JWT 配置嵌套类
     */
    @Data
    public static class JwtConfig {
        /**
         * JWT签名密钥
         * <p>HS256算法要求至少32个字符</p>
         * <p>示例：SecretKey012345678901234567890123456789</p>
         */
        @NotNull
        private String secretKey;
    }

    /**
     * Redis令牌配置嵌套类
     */
    @Data
    public static class RedisTokenConfig {
        /**
         * 是否允许多设备同时登录
         * <p>true - 允许同一账户多设备登录（默认）</p>
         * <p>false - 新登录会使旧令牌失效</p>
         */
        private Boolean allowMultiLogin = true;
    }
}
