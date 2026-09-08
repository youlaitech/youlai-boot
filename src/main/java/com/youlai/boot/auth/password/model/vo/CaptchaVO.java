package com.youlai.boot.auth.password.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 验证码信息
 *
 * @author Ray.Hao
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "验证码信息")
public class CaptchaVO {

    @Schema(description = "验证码缓存ID")
    private String captchaId;

    @Schema(description = "验证码图片Base64字符串")
    private String captchaBase64;

}
