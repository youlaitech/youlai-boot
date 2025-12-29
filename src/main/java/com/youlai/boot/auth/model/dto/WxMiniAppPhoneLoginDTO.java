package com.youlai.boot.auth.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 备份：微信小程序手机号登录请求参数（原文件名包含 DTO 大写后缀）
 *
 * 原内容保留在此备份文件中，以便恢复或参考，但该文件使用 .bak 后缀以避免编译冲突。
 */
@Schema(description = "微信小程序手机号登录请求参数")
@Data
class WxMiniAppPhoneLoginDto {

    @Schema(description = "微信小程序登录时获取的code", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "code不能为空")
    private String code;

    @Schema(description = "包括敏感数据在内的完整用户信息的加密数据")
    private String encryptedData;

    @Schema(description = "加密算法的初始向量")
    private String iv;

}


