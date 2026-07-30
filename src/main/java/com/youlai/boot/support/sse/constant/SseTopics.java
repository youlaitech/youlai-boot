package com.youlai.boot.support.sse.constant;

/**
 * SSE 业务主题常量
 *
 * @author Ray.Hao
 * @since 3.1.0
 */
public final class SseTopics {

    private SseTopics() {
    }

    /** 字典变更事件 */
    public static final String DICT = "dict";

    /** 系统消息事件 */
    public static final String SYSTEM = "system";

    /** 通知事件 */
    public static final String NOTICE = "notice";

    /** 通知撤回事件 */
    public static final String NOTICE_REVOKE = "notice-revoke";
}
