package com.youlai.boot.framework.security.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 安全配置；未选中的令牌策略不要求提供配置
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
@Data
@Validated
@Component
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    @Valid
    @NotNull
    private TokenConfig token = new TokenConfig();

    // 匿名路径仍经过安全过滤器链
    @NotEmpty
    private String[] permitAllUrls;

    /**
     * 访问令牌与会话期限配置
     */
    @Data
    public static class TokenConfig {

        @NotNull
        @Pattern(regexp = "jwt|redis-token", message = "令牌类型只能是 jwt 或 redis-token")
        private String type = "jwt";

        @NotNull
        private Duration accessTokenTimeToLive = Duration.ofHours(2);

        @NotNull
        private Duration refreshTokenTimeToLive = Duration.ofDays(7);

        private boolean allowMultiLogin = true;

        private JwtConfig jwt;

        /**
         * 校验令牌期限为整秒，且访问期限不超过会话期限
         */
        @AssertTrue(message = "令牌有效期须为正整数秒，且访问令牌有效期不能超过刷新令牌有效期")
        public boolean isLifetimeValid() {
            return accessTokenTimeToLive != null && refreshTokenTimeToLive != null
                    && accessTokenTimeToLive.getNano() == 0 && refreshTokenTimeToLive.getNano() == 0
                    && accessTokenTimeToLive.toSeconds() > 0 && refreshTokenTimeToLive.toSeconds() > 0
                    && accessTokenTimeToLive.compareTo(refreshTokenTimeToLive) <= 0;
        }

        /**
         * 校验所选 JWT 策略的签名密钥长度
         */
        @AssertTrue(message = "JWT 模式须配置至少 32 字节的 UTF-8 签名密钥")
        public boolean isSigningKeyValid() {
            return !"jwt".equals(type) || jwt != null && jwt.secretKey != null
                    && jwt.secretKey.getBytes(StandardCharsets.UTF_8).length >= 32;
        }
    }

    /**
     * JWT 签名配置
     */
    @Data
    public static class JwtConfig {
        private String secretKey;
    }
}
