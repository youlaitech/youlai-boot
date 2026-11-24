package com.youlai.boot.framework.web.ratelimit;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 接口限流配置
 *
 * @author Ray.Hao
 * @since 4.3.1
 */
@Data
@Component
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {

    // @RateLimit 未指定 limit 时，窗口内允许的最大请求数
    private int limit = 60;

    // @RateLimit 未指定 window 时使用的滑动窗口
    private Duration window = Duration.ofSeconds(60);

}
