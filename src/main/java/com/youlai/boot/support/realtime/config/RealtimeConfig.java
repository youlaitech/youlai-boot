package com.youlai.boot.support.realtime.config;

import com.youlai.boot.support.realtime.RealtimeWebSocketHandler;
import com.youlai.boot.support.realtime.security.RealtimeHandshakeHandler;
import com.youlai.boot.support.realtime.security.RealtimeHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 注册实时连接端点及专用有界线程池
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSocket
@EnableConfigurationProperties(RealtimeProperties.class)
@RequiredArgsConstructor
public class RealtimeConfig implements WebSocketConfigurer {
    private final RealtimeWebSocketHandler handler;
    private final RealtimeHandshakeHandler handshakeHandler;
    private final RealtimeHandshakeInterceptor interceptor;
    private final RealtimeProperties properties;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        if (!properties.isEnabled()) {
            return;
        }
        registry.addHandler(handler, "/api/v1/realtime/ws")
                .setHandshakeHandler(handshakeHandler::handshake)
                .addInterceptors(interceptor)
                .setAllowedOrigins(properties.getAllowedOrigins().toArray(String[]::new));
    }

    /**
     * 配置 WebSocket 消息上限与写入超时
     */
    @Bean
    public static ServletServerContainerFactoryBean realtimeContainer() {
        var container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(16_384);
        container.setMaxBinaryMessageBufferSize(1_024);
        container.setMaxSessionIdleTimeout(120_000L);
        container.setAsyncSendTimeout(5_000L);
        return container;
    }

    /**
     * 创建有界消息发送线程池
     */
    @Bean(name = "realtimeSenderExecutor", destroyMethod = "shutdown")
    public static ExecutorService senderExecutor() {
        return pool("realtime-send-", 4, 512);
    }

    /**
     * 创建有界连接维护线程池
     */
    @Bean(name = "realtimeMaintenanceExecutor", destroyMethod = "shutdown")
    public static ExecutorService maintenanceExecutor() {
        return pool("realtime-maintain-", 4, 256);
    }

    /**
     * 创建单线程维护调度器
     */
    @Bean(name = "realtimeScheduler", destroyMethod = "shutdown")
    public static ScheduledExecutorService scheduler() {
        return Executors.newSingleThreadScheduledExecutor(threadFactory("realtime-clock-"));
    }

    /**
     * 创建队列有界、拒绝时抛出异常的线程池
     */
    private static ExecutorService pool(String prefix, int threads, int capacity) {
        return new ThreadPoolExecutor(threads, threads, 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(capacity), threadFactory(prefix), new ThreadPoolExecutor.AbortPolicy());
    }

    /**
     * 创建带名称的守护线程
     */
    private static ThreadFactory threadFactory(String prefix) {
        AtomicInteger sequence = new AtomicInteger();
        return task -> {
            Thread thread = new Thread(task, prefix + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }
}
