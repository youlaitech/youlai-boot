package com.youlai.boot.auth.oidc.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * OIDC 登录配置
 *
 * @author Ray.Hao
 * @since 4.12.0
 */
@Data
@Component
@ConditionalOnOidcEnabled
@Validated
@ConfigurationProperties(prefix = "security.oidc")
public class OidcProperties {

    private boolean enabled = false;

    // 身份表中的稳定标识；更换认证中心时使用新值或迁移关联
    @NotBlank
    @Pattern(regexp = "[a-z][a-z0-9-]{0,63}")
    private String provider = "oidc-main";

    // 必须与认证中心公布的 issuer 完全一致
    @NotBlank
    private String issuer;

    @NotBlank
    private String clientId;

    @NotBlank
    private String clientSecret;

    // 认证成功后的前端回调地址
    @NotBlank
    private String frontendCallbackUri;
}
