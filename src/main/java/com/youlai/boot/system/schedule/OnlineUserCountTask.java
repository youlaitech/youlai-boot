package com.youlai.boot.system.schedule;

import com.youlai.boot.support.sse.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 在线用户统计定时任务
 * <p>每 3 分钟统计并广播当前在线用户数量</p>
 *
 * @author Ray.Hao
 * @since 3.1.0
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OnlineUserCountTask {

    private final SseService sseService;

    @Scheduled(cron = "0 */3 * * * ?")
    public void execute() {
        sseService.sendOnlineCount();
    }
}
