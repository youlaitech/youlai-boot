package com.youlai.boot.framework.integration.wxma.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 微信小程序配置属性
 */
@Data
@ConfigurationProperties(prefix = "wx.miniapp")
public class WxMaProperties {

    private String appid;

    private String secret;
}
