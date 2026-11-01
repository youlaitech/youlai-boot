package com.youlai.boot.support.realtime.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

/**
 * 实时通信开关与额外允许的浏览器来源
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
@Getter
@Setter
@ConfigurationProperties("realtime")
public class RealtimeProperties {
    /** 是否启用实时通信接口 */
    private boolean enabled = true;
    /** 额外允许的浏览器来源，如 https://admin.example.com */
    private List<String> allowedOrigins = List.of();
}
