package com.youlai.boot.auth.oidc.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 当前认证中心及用户的关联；未关联时 subject 为 null
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
@Schema(description = "用户的统一认证身份")
public record OidcUserIdentityVO(
        @Schema(description = "服务端配置的认证中心 issuer") String issuer,
        @Schema(description = "已关联的 sub，未关联时为空") String subject
) {
}
