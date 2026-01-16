package com.youlai.boot.system.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "密码校验表单")
@Data
public class PasswordVerifyForm {

    @Schema(description = "当前密码")
    @NotBlank(message = "当前密码不能为空")
    private String password;
}
