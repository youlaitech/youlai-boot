package com.youlai.boot.auth.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 验证码信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "验证码信息")
public class CaptchaInfo {

    @Schema(description = "验证码缓存ID")
    private String captchaId;

    @Schema(description = "验证码图片Base64字符串")
    private String captchaBase64;

}
