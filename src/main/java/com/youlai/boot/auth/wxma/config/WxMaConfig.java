package com.youlai.boot.auth.wxma.config;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 微信小程序配置
 *
 * @author Ray.Hao
 * @since 1.0.0
 */
@Configuration
public class WxMaConfig {

    /**
     * 微信小程序客户端，用于调用 jscode2session、手机号解析等接口
     *
     * @param properties 小程序 appid 与密钥
     * @return WxMaService
     */
    @Bean
    public WxMaService wxMaService(WxMaProperties properties) {
        WxMaDefaultConfigImpl config = new WxMaDefaultConfigImpl();
        config.setAppid(properties.getAppid());
        config.setSecret(properties.getSecret());
        WxMaService service = new WxMaServiceImpl();
        service.setWxMaConfig(config);
        return service;
    }
}
