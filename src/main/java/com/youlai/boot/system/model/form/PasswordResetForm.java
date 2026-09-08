package com.youlai.boot.system.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 重置密码表单
 *
 * @author youlai.tech
 */
@Schema(description = "重置密码表单")
@Data
public class PasswordResetForm {

    @Schema(description = "新密码")
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度需在 6-20 个字符之间")
    private String password;
}