package com.youlai.boot.config.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 限流配置
 * <p>对应 application.yml 中的 {@code rate-limit} 配置节点。</p>
 *
 * @author Ray.Hao
 * @since 4.3.1
 */
@Data
@Component
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {

    /**
     * @RateLimit 注解未显式指定 limit 时的默认阈值（窗口内最大请求数）
     */
    private int defaultLimit = 5;

    /**
     * @RateLimit 注解未显式指定 window 时的默认窗口大小，支持 60s / 1m / 500ms 等 Duration 写法
     */
    private Duration defaultWindow = Duration.ofSeconds(60);

    /**
     * IP 全局限流配置
     */
    private Ip ip = new Ip();

    @Data
    public static class Ip {

        /**
         * 是否启用 IP 全局限流。
         * 建议生产环境开启，开发/测试环境酌情关闭。
         */
        private boolean enabled = true;

        /**
         * 窗口内最大请求数（默认 1000）
         */
        private int limit = 1000;

        /**
         * 滑动窗口大小，支持 60s / 1m / 500ms 等 Duration 写法（默认 60s）
         */
        private Duration window = Duration.ofSeconds(60);

    }

}
