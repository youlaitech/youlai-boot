package com.youlai.boot.message.job;

import com.youlai.boot.message.registry.SseSessionRegistry;
import com.youlai.boot.message.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 在线用户数统计定时任务
 * <p>
 * 定时统计并广播当前在线用户数量到所有 SSE 客户端
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OnlineUserCountJob {

    private final SseSessionRegistry sessionRegistry;
    private final SseService sseService;

    /**
     * 定时统计在线用户数并广播
     * <p>
     * 每3分钟执行一次，推送当前在线用户数量
     */
    @Scheduled(cron = "0 */3 * * * ?")
    public void execute() {
        int onlineCount = sessionRegistry.getOnlineUserCount();
        int connectionCount = sessionRegistry.getTotalConnectionCount();

        log.debug("定时统计：在线用户数={}, 总连接数={}", onlineCount, connectionCount);

        // 发送在线用户数量
        sseService.sendOnlineCount();
    }
}
