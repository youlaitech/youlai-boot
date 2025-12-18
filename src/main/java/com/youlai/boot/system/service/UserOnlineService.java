package com.youlai.boot.system.service;

import com.youlai.boot.platform.websocket.publisher.WebSocketPublisher;
import com.youlai.boot.platform.websocket.topic.WebSocketTopics;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 用户在线状态服务
 * 负责维护用户的在线状态和相关统计
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
@Service
@Slf4j
public class UserOnlineService {

    // 在线用户映射表，key为用户名，value为用户在线信息
    private final Map<String, UserOnlineInfo> onlineUsers = new ConcurrentHashMap<>();
    
    private final WebSocketPublisher webSocketPublisher;

    public UserOnlineService(WebSocketPublisher webSocketPublisher) {
        this.webSocketPublisher = webSocketPublisher;
    }

    /**
     * 用户上线
     *
     * @param username  用户名
     * @param sessionId WebSocket会话ID（可选）
     */
    public void userConnected(String username, String sessionId) {
        // 生成会话ID（如果未提供）
        String actualSessionId = sessionId != null ? sessionId : "session-" + System.nanoTime();
        UserOnlineInfo info = new UserOnlineInfo(username, actualSessionId, System.currentTimeMillis());
        onlineUsers.put(username, info);
        log.info("用户[{}]上线，当前在线用户数：{}", username, onlineUsers.size());
        
        // 通知在线用户状态变更
        notifyOnlineUsersChange();
    }

    /**
     * 用户下线
     *
     * @param username 用户名
     */
    public void userDisconnected(String username) {
        onlineUsers.remove(username);
        log.info("用户[{}]下线，当前在线用户数：{}", username, onlineUsers.size());
        
        // 通知在线用户状态变更
        notifyOnlineUsersChange();
    }

    /**
     * 获取在线用户列表
     *
     * @return 在线用户名列表
     */
    public List<UserOnlineDto> getOnlineUsers() {
        return onlineUsers.values().stream()
                .map(info -> new UserOnlineDto(info.getUsername(), info.getLoginTime()))
                .collect(Collectors.toList());
    }

    /**
     * 获取在线用户数量
     *
     * @return 在线用户数
     */
    public int getOnlineUserCount() {
        return onlineUsers.size();
    }

    /**
     * 检查用户是否在线
     *
     * @param username 用户名
     * @return 是否在线
     */
    public boolean isUserOnline(String username) {
        return onlineUsers.containsKey(username);
    }

    /**
     * 通知所有客户端在线用户变更
     */
    private void notifyOnlineUsersChange() {
        // 发送简化版数据（仅数量）
        sendOnlineUserCount();
    }
    
    /**
     * 发送在线用户数量（简化版，不包含用户详情）
     */
    private void sendOnlineUserCount() {
        try {
            // 直接发送数量，更轻量
            int count = onlineUsers.size();
            webSocketPublisher.publish(WebSocketTopics.TOPIC_ONLINE_COUNT, count);
            log.debug("已发送在线用户数量: {}", count);
        } catch (Exception e) {
            log.error("发送在线用户数量失败", e);
        }
    }

    /**
     * 用户在线信息
     */
    @Data
    private static class UserOnlineInfo {
        private final String username;
        private final String sessionId;
        private final long loginTime;
    }

    /**
     * 用户在线Dto（用于返回给前端）
     */
    @Data
    public static class UserOnlineDto {
        private final String username;
        private final long loginTime;
    }

    /**
     * 在线用户变更事件
     */
    @Data
    private static class OnlineUsersChangeEvent {
        private String type;
        private int count;
        private List<UserOnlineDto> users;
        private long timestamp;
    }
} 
