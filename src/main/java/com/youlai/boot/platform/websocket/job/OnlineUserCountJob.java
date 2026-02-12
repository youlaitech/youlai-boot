package com.youlai.boot.platform.websocket.job;

import com.youlai.boot.platform.websocket.publisher.WebSocketPublisher;
import com.youlai.boot.platform.websocket.session.UserSessionRegistry;
import com.youlai.boot.platform.websocket.topic.WebSocketTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 在线用户数统计定时任务
 * <p>
 * 定时统计并广播当前在线用户数量到所有WebSocket客户端。
 * 用于解决以下问题：
 * <ul>
 *   <li>客户端页面刷新后可快速同步最新在线人数</li>
 *   <li>减少服务端主动推送频率，降低资源消耗</li>
 * </ul>
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OnlineUserCountJob {

    private final UserSessionRegistry userSessionRegistry;
    private final WebSocketPublisher webSocketPublisher;

    /**
     * 定时统计在线用户数并广播
     * <p>
     * 每3分钟执行一次，推送当前在线用户数量
     */
    @Scheduled(cron = "0 */3 * * * ?")
    public void execute() {
        int onlineCount = userSessionRegistry.getOnlineUserCount();
        int sessionCount = userSessionRegistry.getTotalSessionCount();

        log.debug("定时统计：在线用户数={}, 总会话数={}", onlineCount, sessionCount);

        // 广播在线用户数量
        webSocketPublisher.publish(WebSocketTopics.TOPIC_ONLINE_COUNT, onlineCount);
    }
}
