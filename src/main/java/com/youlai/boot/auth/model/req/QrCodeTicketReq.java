package com.youlai.boot.auth.model.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 扫码登录票据请求，用于 scan/confirm/cancel/login 接口
 *
 * @author Ray.Hao
 * @since 4.5.0
 */
@Data
public class QrCodeTicketReq {

    @NotBlank(message = "票据不能为空")
    private String ticket;
}
