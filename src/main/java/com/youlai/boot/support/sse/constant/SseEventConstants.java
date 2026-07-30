package com.youlai.boot.support.sse.constant;

/**
 * SSE 通用事件常量
 *
 * @author Ray.Hao
 * @since 3.1.0
 */
public final class SseEventConstants {

    private SseEventConstants() {
    }

    /** 心跳事件 */
    public static final String PING = "ping";

    /** 在线用户数事件 */
    public static final String ONLINE_USERS = "online-users";
}
