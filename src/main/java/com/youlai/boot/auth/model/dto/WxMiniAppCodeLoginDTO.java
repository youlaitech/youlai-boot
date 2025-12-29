package com.youlai.boot.auth.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 备份：微信小程序Code登录请求参数（原文件名包含 DTO 大写后缀）
 *
 * 原内容保留在此备份文件中，以便恢复或参考，但该文件使用 .bak 后缀以避免编译冲突。
 */
@Schema(description = "微信小程序Code登录请求参数")
@Data
class WxMiniAppCodeLoginDto {

    @Schema(description = "微信小程序登录时获取的code", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "code不能为空")
    private String code;

}


