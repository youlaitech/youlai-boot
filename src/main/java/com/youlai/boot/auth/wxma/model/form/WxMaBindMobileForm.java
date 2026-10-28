package com.youlai.boot.auth.wxma.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 微信小程序绑定手机号请求
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
@Schema(description = "微信小程序绑定手机号请求")
@Data
public class WxMaBindMobileForm {

    // 提交绑定前重新调用 wx.login，服务端据此确认 OpenID，不接受客户端指定身份
    @NotBlank(message = "微信登录凭证不能为空")
    @Schema(description = "wx.login 返回的一次性 code")
    private String loginCode;

    @NotBlank(message = "手机号不能为空")
    @Schema(description = "手机号码", example = "18888888888")
    private String mobile;

    @NotBlank(message = "短信验证码不能为空")
    @Schema(description = "短信验证码", example = "123456")
    private String smsCode;
}
