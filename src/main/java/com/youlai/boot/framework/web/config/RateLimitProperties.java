package com.youlai.boot.framework.web.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 限流配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {

    private int defaultLimit = 5;

    private int defaultWindow = 1;
}
