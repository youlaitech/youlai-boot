package com.youlai.boot.support.realtime.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.Set;

/**
 * 客户端实例、运行平台与本次连接的订阅主题
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
public record CreateRealtimeTicketRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{16,128}") String clientInstanceId,
        @NotBlank @Pattern(regexp = "web|mp-weixin|app") String platform,
        @NotNull Set<String> topics) {
    /**
     * 订阅须包含 notice，且只允许已支持的主题
     */
    public CreateRealtimeTicketRequest {
        if (topics != null) {
            if (!topics.contains("notice") || !Set.of("notice", "dict", "presence").containsAll(topics)) {
                throw new IllegalArgumentException("订阅主题须包含 notice，且仅支持 notice、dict、presence");
            }
            topics = Set.copyOf(topics);
        }
    }
}
