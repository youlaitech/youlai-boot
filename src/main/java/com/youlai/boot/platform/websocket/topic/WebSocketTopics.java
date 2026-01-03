package com.youlai.boot.platform.websocket.topic;

public final class WebSocketTopics {

    private WebSocketTopics() {
    }

    public static final String TOPIC_DICT = "/topic/dict";
    public static final String TOPIC_ONLINE_COUNT = "/topic/online-count";
    public static final String TOPIC_PUBLIC = "/topic/public";

    public static final String USER_QUEUE_MESSAGES = "/queue/messages";
    public static final String USER_QUEUE_MESSAGE = "/queue/message";
    public static final String USER_QUEUE_GREETING = "/queue/greeting";
}
