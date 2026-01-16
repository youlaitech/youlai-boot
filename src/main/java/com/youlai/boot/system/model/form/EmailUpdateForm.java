package com.youlai.boot.system.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 修改邮箱表单
 *
 * @author Ray.Hao
 * @since 2024/8/19
 */
@Schema(description = "修改邮箱表单")
@Data
public class EmailUpdateForm {

    @Schema(description = "邮箱")
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Schema(description = "验证码")
    @NotBlank(message = "验证码不能为空")
    private String code;

    @Schema(description = "当前密码")
    @NotBlank(message = "当前密码不能为空")
    private String password;

}
