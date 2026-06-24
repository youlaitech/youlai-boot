package com.youlai.boot.message.job;

import com.youlai.boot.message.registry.SseSessionRegistry;
import com.youlai.boot.message.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 在线用户统计
 * <p>定时统计并广播当前在线用户数量</p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OnlineUserCountTask {

    private final SseSessionRegistry sessionRegistry;
    private final SseService sseService;

    @Scheduled(cron = "0 */3 * * * ?")
    public void execute() {
        int onlineCount = sessionRegistry.getOnlineUserCount();
        int connectionCount = sessionRegistry.getTotalConnectionCount();
        log.debug("在线用户数={} 总连接数={}", onlineCount, connectionCount);
        sseService.sendOnlineCount();
    }
}
