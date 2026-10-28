package com.youlai.boot.auth.wxma.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 微信小程序登录响应
 *
 * @author Ray.Hao
 * @since 4.13.0
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

    @Schema(description = "访问令牌")
    @ToString.Exclude
    private String accessToken;

    @Schema(description = "刷新令牌")
    @ToString.Exclude
    private String refreshToken;

    @Schema(description = "令牌类型")
    private String tokenType;

    @Schema(description = "过期时间（秒）")
    private Long expiresIn;
}
