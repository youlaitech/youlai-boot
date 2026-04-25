package com.youlai.boot.message.registry;

import com.youlai.boot.message.dto.OnlineUserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * SSE 会话注册表
 * <p>
 * 维护 SSE 连接的用户会话信息，支持多设备同时登录
 */
@Slf4j
@Component
public class SseSessionRegistry {

    /** 用户名 -> SseEmitter 集合（支持多设备） */
    private final Map<String, Set<SseEmitter>> userEmittersMap = new ConcurrentHashMap<>();

    /** SseEmitter -> 用户名（快速定位用户） */
    private final Map<SseEmitter, String> emitterUserMap = new ConcurrentHashMap<>();

    /** SseEmitter -> 连接时间 */
    private final Map<SseEmitter, Long> emitterTimeMap = new ConcurrentHashMap<>();

    /**
     * 用户上线（建立 SSE 连接）
     *
     * @param username 用户名
     * @param emitter  SseEmitter
     */
    public void userConnected(String username, SseEmitter emitter) {
        userEmittersMap.computeIfAbsent(username, k -> ConcurrentHashMap.newKeySet()).add(emitter);
        emitterUserMap.put(emitter, username);
        emitterTimeMap.put(emitter, System.currentTimeMillis());
        log.debug("用户[{}]SSE连接已建立", username);

        // 设置连接超时和完成回调
        emitter.onCompletion(() -> {
            removeEmitter(emitter);
            log.debug("用户[{}]SSE连接已完成", username);
        });
        emitter.onTimeout(() -> {
            removeEmitter(emitter);
            log.debug("用户[{}]SSE连接超时", username);
        });
        emitter.onError(e -> {
            removeEmitter(emitter);
            log.debug("用户[{}]SSE连接错误: {}", username, e.getMessage());
        });
    }

    /**
     * 移除指定 emitter
     */
    private void removeEmitter(SseEmitter emitter) {
        String username = emitterUserMap.remove(emitter);
        if (username == null) {
            return;
        }
        emitterTimeMap.remove(emitter);

        Set<SseEmitter> emitters = userEmittersMap.get(username);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                userEmittersMap.remove(username);
                log.debug("用户[{}]所有SSE连接已断开", username);
            }
        }
    }

    /**
     * 用户下线（断开所有 SSE 连接）
     *
     * @param username 用户名
     */
    public void userDisconnected(String username) {
        Set<SseEmitter> emitters = userEmittersMap.remove(username);
        if (emitters == null) {
            return;
        }
        emitters.forEach(emitter -> {
            emitterUserMap.remove(emitter);
            emitterTimeMap.remove(emitter);
            try {
                emitter.complete();
            } catch (Exception ignored) {
            }
        });
        log.debug("用户[{}]已下线，移除{}个SSE连接", username, emitters.size());
    }

    /**
     * 获取在线用户数量
     */
    public int getOnlineUserCount() {
        return userEmittersMap.size();
    }

    /**
     * 获取总连接数量（包括多设备）
     */
    public int getTotalConnectionCount() {
        return emitterUserMap.size();
    }

    /**
     * 获取指定用户的连接数量
     */
    public int getUserConnectionCount(String username) {
        Set<SseEmitter> emitters = userEmittersMap.get(username);
        return emitters != null ? emitters.size() : 0;
    }

    /**
     * 检查用户是否在线
     */
    public boolean isUserOnline(String username) {
        Set<SseEmitter> emitters = userEmittersMap.get(username);
        return emitters != null && !emitters.isEmpty();
    }

    /**
     * 获取所有在线用户列表
     */
    public List<OnlineUserDTO> getOnlineUsers() {
        return userEmittersMap.entrySet().stream()
                .map(entry -> {
                    String username = entry.getKey();
                    Set<SseEmitter> emitters = entry.getValue();
                    long earliestTime = emitters.stream()
                            .map(emitterTimeMap::get)
                            .filter(t -> t != null)
                            .mapToLong(Long::longValue)
                            .min()
                            .orElse(System.currentTimeMillis());
                    return new OnlineUserDTO(username, emitters.size(), earliestTime);
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取所有活跃的 SseEmitter
     */
    public Set<SseEmitter> getAllEmitters() {
        return emitterUserMap.keySet();
    }

    /**
     * 获取指定用户的所有 SseEmitter
     */
    public Set<SseEmitter> getUserEmitters(String username) {
        return userEmittersMap.get(username);
    }

    /**
     * 向指定 emitter 发送事件
     */
    public boolean sendEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
            return true;
        } catch (IOException e) {
            log.warn("发送SSE事件失败: {}", e.getMessage());
            removeEmitter(emitter);
            return false;
        }
    }

    /**
     * 向所有连接广播事件
     */
    public void broadcast(String eventName, Object data) {
        getAllEmitters().forEach(emitter -> sendEvent(emitter, eventName, data));
    }

    /**
     * 向指定用户发送事件
     */
    public void sendToUser(String username, String eventName, Object data) {
        Set<SseEmitter> emitters = userEmittersMap.get(username);
        if (emitters != null) {
            emitters.forEach(emitter -> sendEvent(emitter, eventName, data));
        }
    }

    /**
     * 心跳检测：每30秒向所有连接发送ping事件，及时清理已断开的僵尸连接
     */
    @Scheduled(fixedRate = 30000)
    public void heartbeat() {
        if (emitterUserMap.isEmpty()) {
            return;
        }
        List<SseEmitter> failedEmitters = new ArrayList<>();
        for (SseEmitter emitter : emitterUserMap.keySet()) {
            try {
                emitter.send(SseEmitter.event().name("ping").data("heartbeat"));
            } catch (Exception e) {
                failedEmitters.add(emitter);
            }
        }
        if (!failedEmitters.isEmpty()) {
            log.debug("心跳检测清理{}个失效SSE连接", failedEmitters.size());
            failedEmitters.forEach(this::removeEmitter);
        }
    }

    /**
     * 容器关闭时主动断开所有 SSE 连接，避免阻塞应用停止
     */
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @EventListener(ContextClosedEvent.class)
    public void destroy() {
        int count = emitterUserMap.size();
        if (count == 0) {
            return;
        }
        log.info("应用关闭，主动断开 {} 个SSE连接...", count);
        emitterUserMap.keySet().forEach(emitter -> {
            try {
                emitter.complete();
            } catch (Exception ignored) {
            }
        });
        userEmittersMap.clear();
        emitterUserMap.clear();
        emitterTimeMap.clear();
        log.info("所有SSE连接已断开");
    }
}
