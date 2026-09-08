package com.youlai.boot.auth.wxma.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 微信小程序配置属性
 *
 * @author Ray.Hao
 */
@Data
@ConfigurationProperties(prefix = "wx.miniapp")
public class WxMaProperties {

    /** 小程序 appid */
    private String appid;

    /** 小程序 appsecret */
    private String secret;
}
