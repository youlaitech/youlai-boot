package com.youlai.boot.framework.integration.wxma;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 微信小程序配置
 *
 * @author Ray.Hao
 * @since 2024/01/01
 */
@Configuration
@EnableConfigurationProperties(WxMaProperties.class)
public class WxMaConfig {

    /**
     * 微信小程序服务
     *
     * @param properties 微信小程序配置属性
     * @return {@link WxMaService}
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
