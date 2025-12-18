package com.youlai.boot.auth.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 微信小程序Code登录请求参数
 *
 * @author 有来技术团队
 * @since 2.0.0
 */
@Schema(description = "微信小程序Code登录请求参数")
@Data
public class WxMiniAppCodeLoginDto {

    @Schema(description = "微信小程序登录时获取的code", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "code不能为空")
    private String code;

} 