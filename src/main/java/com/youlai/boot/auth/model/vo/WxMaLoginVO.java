package com.youlai.boot.auth.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 微信小程序登录响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "微信小程序登录响应")
public class WxMaLoginVO {

    @Schema(description = "是否新用户")
    private Boolean isNewUser;

    @Schema(description = "是否需要绑定手机号")
    private Boolean needBindMobile;

    @Schema(description = "微信openid")
    private String openid;

    @Schema(description = "访问令牌")
    private String accessToken;

    @Schema(description = "刷新令牌")
    private String refreshToken;

    @Schema(description = "令牌类型")
    private String tokenType;

    @Schema(description = "过期时间（秒）")
    private Integer expiresIn;
}
