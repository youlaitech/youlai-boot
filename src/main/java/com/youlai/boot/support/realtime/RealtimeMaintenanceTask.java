package com.youlai.boot.support.realtime;

import com.youlai.boot.support.realtime.config.RealtimeProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 调度实时连接的心跳与授权维护
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
@Slf4j
@Component
public class RealtimeMaintenanceTask {
    private final ScheduledExecutorService scheduler;
    private final RealtimeSessionManager sessions;
    private final RealtimeProperties properties;
    private ScheduledFuture<?> task;

    public RealtimeMaintenanceTask(@Qualifier("realtimeScheduler") ScheduledExecutorService scheduler,
                                   RealtimeSessionManager sessions, RealtimeProperties properties) {
        this.scheduler = scheduler;
        this.sessions = sessions;
        this.properties = properties;
    }

    /**
     * 启动定期维护任务
     */
    @PostConstruct
    public void start() {
        if (!properties.isEnabled()) {
            return;
        }
        task = scheduler.scheduleWithFixedDelay(() -> {
            try {
                sessions.maintain();
            } catch (Exception exception) {
                log.error("Realtime maintenance failed", exception);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * 取消维护任务
     */
    @PreDestroy
    public void stop() {
        if (task != null) {
            task.cancel(false);
        }
    }
}
