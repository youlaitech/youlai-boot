package com.youlai.boot.support.realtime.protocol;

import java.util.UUID;

/**
 * 带协议版本和消息标识的服务端消息
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
public record RealtimeServerMessage(int version, String id, String type, Object payload) {
    /**
     * 为服务端消息生成唯一标识
     */
    public static RealtimeServerMessage create(String type, Object payload) {
        return new RealtimeServerMessage(1, UUID.randomUUID().toString(), type, payload);
    }
}
