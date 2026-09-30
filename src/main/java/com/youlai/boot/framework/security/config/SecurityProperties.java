package com.youlai.boot.framework.security.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import org.springframework.stereotype.Component;

/**
 * 安全模块配置属性类
 *
 * @author Ray.Hao
 * @since 2024/4/18
 */
@Data
@Validated
@Component
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    // 令牌策略配置
    @Valid
    @NotNull(message = "令牌策略配置不能为空")
    private TokenConfig token;

    // 匿名放行路径（仍经过安全过滤器链，仅跳过认证与授权判断，保留安全响应头）
    @NotEmpty
    private String[] permitAllUrls;

    /**
     * 令牌策略嵌套类
     */
    @Data
    public static class TokenConfig {
        // 令牌策略类型
        @NotNull(message = "令牌类型不能为空")
        @Pattern(regexp = "jwt|redis-token", message = "令牌类型只能是 jwt 或 redis-token")
        private String type;

        // 访问令牌有效期（单位：秒）
        @Min(-1)
        private Integer accessTokenTimeToLive = 3600;

        // 刷新令牌有效期（单位：秒）
        @Min(-1)
        private Integer refreshTokenTimeToLive = 604800;

        // JWT 配置项（type 为 jwt 时必填）
        @Valid
        private JwtConfig jwt;

        // Redis令牌配置项（type 为 redis-token 时使用）
        @Valid
        private RedisTokenConfig redisToken;
    }

    /**
     * JWT 配置嵌套类
     */
    @Data
    public static class JwtConfig {
        // JWT签名密钥
        @NotNull(message = "JWT 密钥不能为空")
        private String secretKey;
    }

    /**
     * Redis令牌配置嵌套类
     */
    @Data
    public static class RedisTokenConfig {
        // 是否允许多设备同时登录
        private Boolean allowMultiLogin = true;
    }
}
