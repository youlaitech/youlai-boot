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

    /** 是否启用 OIDC 统一认证；启用后必须配置认证中心、客户端凭据和回跳地址 */
    private boolean enabled = false;

    /** 身份关联使用的稳定标识；更换认证中心时使用新值或迁移已有关联 */
    @NotBlank
    @Pattern(regexp = "[a-z][a-z0-9-]{0,63}")
    private String provider = "oidc-main";

    /** 认证中心地址，必须与其公布的 issuer 完全一致 */
    @NotBlank
    private String issuer;

    /** 认证中心注册的客户端 ID，启用时必填 */
    @NotBlank
    private String clientId;

    /** 客户端密钥，启用时必填；当前使用 client_secret_basic 认证 */
    @NotBlank
    private String clientSecret;

    /** 认证成功后的前端回跳地址，启用时必填 */
    @NotBlank
    private String frontendCallbackUri;
}
