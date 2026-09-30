package com.youlai.boot.auth.wxma.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信小程序配置属性
 *
 * @author Ray.Hao
 */
@Data
@Component
@ConfigurationProperties(prefix = "wx.miniapp")
public class WxMaProperties {

    // 小程序 appid
    private String appid;

    // 小程序 appsecret
    private String secret;
}
