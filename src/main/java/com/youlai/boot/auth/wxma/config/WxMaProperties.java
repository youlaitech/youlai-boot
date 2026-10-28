package com.youlai.boot.auth.wxma.config;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * 微信小程序配置属性
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "wx.miniapp")
public class WxMaProperties {

    /**
     * 当前小程序的固定认证源标识；不同 AppID 使用不同标识，不能复用已有账号关联
     */
    @NotBlank
    @Pattern(regexp = "[a-z][a-z0-9-]{0,63}")
    private String provider = "wechat-mini-main";

    // 小程序 appid
    private String appid;

    // 小程序 appsecret
    private String secret;
}
