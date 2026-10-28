package com.youlai.boot.auth.oidc.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 统一认证票据兑换请求
 *
 * @author Ray.Hao
 * @since 4.12.0
 */
@Schema(description = "统一认证票据兑换请求")
@Data
public class OidcTicketExchangeForm {

    @Schema(description = "一次性交接票据")
    @NotBlank(message = "票据不能为空")
    private String ticket;
}
