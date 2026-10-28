package com.youlai.boot.auth.oidc.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 设置本地用户的 OIDC 身份；issuer 和 provider 均由服务端配置确定
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
@Data
@Schema(description = "用户的统一认证身份设置")
public class OidcUserIdentityForm {

    // 认证中心实际签发的 sub，区分大小写，不能填写用户名或客户端 ID
    @NotBlank(message = "subject 不能为空")
    @Size(max = 255, message = "subject 长度不能超过255个字符")
    @Schema(description = "ID Token 中的 sub")
    private String subject;
}
