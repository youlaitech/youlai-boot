package com.youlai.boot.auth.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求参数
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
@Schema(description = "登录请求参数")
@Data
public class LoginRequest {

    @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED, example = "admin")
    @NotBlank(message = "用户名不能为空")
    private String username;

    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
    @NotBlank(message = "密码不能为空")
    private String password;

    @Schema(description = "验证码缓存ID", example = "captcha_id_123")
    private String captchaId;

    @Schema(description = "验证码", example = "1234")
    private String captchaCode;

    @Schema(description = "租户ID（可选，多租户模式下用于指定租户）", example = "1")
    private Long tenantId;
}

