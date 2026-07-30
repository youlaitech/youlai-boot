package com.youlai.boot.auth.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 微信小程序绑定手机号请求
 */
@Schema(description = "微信小程序绑定手机号请求")
@Data
public class WxMaBindMobileReq {

    @NotBlank(message = "openid 不能为空")
    @Schema(description = "微信用户唯一标识", example = "oVBkZ0aYgDMDIywRdgPW8-joxXc4")
    private String openid;

    @NotBlank(message = "手机号不能为空")
    @Schema(description = "手机号码", example = "18888888888")
    private String mobile;

    @NotBlank(message = "短信验证码不能为空")
    @Schema(description = "短信验证码", example = "123456")
    private String smsCode;
}
