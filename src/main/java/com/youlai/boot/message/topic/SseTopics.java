package com.youlai.boot.message.topic;

/**
 * SSE 主题常量
 */
public final class SseTopics {

    private SseTopics() {
    }

    /** 字典变更事件 */
    public static final String DICT = "dict";

    /** 在线用户数事件 */
    public static final String ONLINE_COUNT = "online-count";

    /** 系统消息事件 */
    public static final String SYSTEM = "system";
}
