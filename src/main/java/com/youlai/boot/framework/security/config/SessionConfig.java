package com.youlai.boot.framework.security.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * 会话基础组件配置
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
@Configuration(proxyBeanMethods = false)
public class SessionConfig {

    /**
     * 提供认证使用的 UTC 时钟
     */
    @Bean
    @ConditionalOnMissingBean
    public Clock securityClock() {
        return Clock.systemUTC();
    }
}
