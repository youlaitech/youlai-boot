package com.youlai.boot.auth.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * generate 接口响应
 *
 * @author Ray.Hao
 * @since 4.5.0
 */
@Data
@Builder
@Schema(description = "扫码票据生成结果")
public class QrCodeGenerateResp {

    @Schema(description = "票据")
    private String ticket;

    @Schema(description = "有效期（秒）")
    private Integer expireSeconds;
}
