package com.youlai.boot.auth.qrcode.model.form;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 扫码登录票据表单，用于 scan/confirm/cancel/login 接口。
 */
@Data
public class QrCodeTicketForm {

    @NotBlank(message = "票据不能为空")
    private String ticket;
}
