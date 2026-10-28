package com.youlai.boot.system.event;

/**
 * 通知变更事件
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
public record NoticeChangedEvent(Long noticeId, String action) {
}
