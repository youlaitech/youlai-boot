package com.youlai.boot.platform.websocket.service.impl;

import com.youlai.boot.platform.websocket.dto.DictChangeEvent;
import com.youlai.boot.platform.websocket.dto.TextMessage;
import com.youlai.boot.platform.websocket.publisher.WebSocketPublisher;
import com.youlai.boot.platform.websocket.session.UserSessionRegistry;
import com.youlai.boot.platform.websocket.service.WebSocketService;
import com.youlai.boot.platform.websocket.topic.WebSocketTopics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * WebSocket 服务实现类
 * 
 * 核心功能：
 * - 用户在线状态管理（支持多设备登录）
 * - 消息推送（广播、点对点）
 * - 字典变更通知
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
@Service
@Slf4j
public class WebSocketServiceImpl implements WebSocketService {

    private final UserSessionRegistry userSessionRegistry;
    private final WebSocketPublisher webSocketPublisher;

    public WebSocketServiceImpl(UserSessionRegistry userSessionRegistry, WebSocketPublisher webSocketPublisher) {
        this.userSessionRegistry = userSessionRegistry;
        this.webSocketPublisher = webSocketPublisher;
    }

    // ==================== 用户在线状态管理 ====================

    /**
     * 处理用户连接事件
     *
     * @param username  用户名
     * @param sessionId WebSocket 会话 ID
     */
    @Override
    public void userConnected(String username, String sessionId) {
        if (username == null || username.isEmpty()) {
            log.warn("用户连接失败：用户名为空");
            return;
        }

        if (sessionId == null || sessionId.isEmpty()) {
            log.warn("用户[{}]连接失败：会话 ID 为空", username);
            return;
        }

        userSessionRegistry.userConnected(username, sessionId);

        int sessionCount = userSessionRegistry.getUserSessionCount(username);
        int totalOnlineUsers = userSessionRegistry.getOnlineUserCount();

        log.info("✓ 用户[{}]会话[{}]上线（该用户共 {} 个会话，系统总在线用户数：{}）",
                username, sessionId, sessionCount, totalOnlineUsers);

        // 广播在线用户数变更
        broadcastOnlineUserCount();
    }

    /**
     * 处理用户断开连接事件
     *
     * @param username 用户名
     */
    @Override
    public void userDisconnected(String username) {
        if (username == null || username.isEmpty()) {
            return;
        }

        userSessionRegistry.userDisconnected(username);

        int totalOnlineUsers = userSessionRegistry.getOnlineUserCount();
        log.info("✓ 用户[{}]下线（系统总在线用户数：{}）", username, totalOnlineUsers);

        // 广播在线用户数变更
        broadcastOnlineUserCount();
    }

    /**
     * 移除指定会话（单个设备下线）
     *
     * @param sessionId 会话 ID
     */
    public void removeSession(String sessionId) {
        userSessionRegistry.removeSession(sessionId);
        broadcastOnlineUserCount();
    }

    /**
     * 获取在线用户列表
     *
     * @return 在线用户信息列表
     */
    public List<UserSessionRegistry.OnlineUserDto> getOnlineUsers() {
        return userSessionRegistry.getOnlineUsers();
    }

    /**
     * 获取在线用户数量
     *
     * @return 在线用户数（不是会话数）
     */
    public int getOnlineUserCount() {
        return userSessionRegistry.getOnlineUserCount();
    }

    /**
     * 获取在线会话总数
     *
     * @return 所有在线会话的总数
     */
    public int getTotalSessionCount() {
        return userSessionRegistry.getTotalSessionCount();
    }

    /**
     * 检查用户是否在线
     *
     * @param username 用户名
     * @return 是否在线
     */
    public boolean isUserOnline(String username) {
        return userSessionRegistry.isUserOnline(username);
    }

    /**
     * 获取指定用户的会话数量
     *
     * @param username 用户名
     * @return 会话数量
     */
    public int getUserSessionCount(String username) {
        return userSessionRegistry.getUserSessionCount(username);
    }

    /**
     * 手动触发在线用户数量广播
     * 
     * 供外部服务（如定时任务）调用
     */
    public void notifyOnlineUsersChange() {
        log.info("手动触发在线用户数量通知，当前在线用户数：{}", getOnlineUserCount());
        broadcastOnlineUserCount();
    }

    /**
     * 广播在线用户数量变更（内部方法）
     */
    private void broadcastOnlineUserCount() {
        int count = getOnlineUserCount();
        webSocketPublisher.publish(WebSocketTopics.TOPIC_ONLINE_COUNT, count);
        log.debug("✓ 已广播在线用户数量: {}", count);
    }

    // ==================== 消息推送功能 ====================

    /**
     * 向所有客户端广播字典更新事件
     *
     * @param dictCode 字典编码
     */
    @Override
    public void broadcastDictChange(String dictCode) {
        if (dictCode == null || dictCode.isEmpty()) {
            log.warn("字典编码为空，跳过广播");
            return;
        }

        DictChangeEvent event = new DictChangeEvent(dictCode);
        webSocketPublisher.publish(WebSocketTopics.TOPIC_DICT, event);
        log.info("✓ 已广播字典变更通知: dictCode={}", dictCode);
    }

    /**
     * 向特定用户发送通知消息
     *
     * @param username 目标用户名
     * @param message  消息内容
     */
    @Override
    public void sendNotification(String username, Object message) {
        if (username == null || username.isEmpty()) {
            log.warn("用户名为空，无法发送通知");
            return;
        }

        if (message == null) {
            log.warn("消息内容为空，无法发送给用户[{}]", username);
            return;
        }

        webSocketPublisher.publishToUser(username, WebSocketTopics.USER_QUEUE_MESSAGES, message);
        log.info("✓ 已向用户[{}]发送通知", username);
    }

    /**
     * 广播系统消息给所有用户
     *
     * @param message 消息内容
     */
    public void broadcastSystemMessage(String message) {
        if (message == null || message.isEmpty()) {
            log.warn("消息内容为空，无法广播");
            return;
        }

        TextMessage systemMessage = new TextMessage("系统通知", message, System.currentTimeMillis());
        webSocketPublisher.publish(WebSocketTopics.TOPIC_PUBLIC, systemMessage);
        log.info("✓ 已广播系统消息: {}", message);
    }
}
