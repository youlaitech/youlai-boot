package com.youlai.boot.auth.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * status/scan/confirm/cancel 接口响应
 *
 * @author Ray.Hao
 * @since 4.5.0
 */
@Data
@Builder
@Schema(description = "扫码登录状态")
public class QrCodeStatusResp {

    @Schema(description = "票据")
    private String ticket;

    @Schema(description = "状态：WAITING/SCANNED/CONFIRMED/LOGGED_IN/CANCELED/EXPIRED")
    private String status;

    @Schema(description = "脱敏昵称，SCANNED 之后返回")
    private String nickname;

    @Schema(description = "头像 URL，SCANNED 之后返回")
    private String avatar;

    @Schema(description = "剩余有效期（秒）")
    private Integer expireSeconds;
}
