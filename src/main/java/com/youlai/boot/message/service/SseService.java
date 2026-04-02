package com.youlai.boot.message.service;

import com.youlai.boot.message.dto.DictChangeEvent;
import com.youlai.boot.message.dto.OnlineUserDTO;
import com.youlai.boot.message.registry.SseSessionRegistry;
import com.youlai.boot.message.topic.SseTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * SSE 服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SseService {

    /** SSE 连接超时时间：30 分钟 */
    private static final long TIMEOUT = 30 * 60 * 1000L;

    private final SseSessionRegistry sessionRegistry;

    /**
     * 创建 SSE 连接
     *
     * @param username 用户名
     * @return SseEmitter
     */
    public SseEmitter createConnection(String username) {
        if (username == null || username.isEmpty()) {
            log.warn("创建SSE连接失败：用户名为空");
            return null;
        }

        // 创建 SseEmitter，设置超时时间
        SseEmitter emitter = new SseEmitter(TIMEOUT);

        // 注册用户连接
        sessionRegistry.userConnected(username, emitter);

        // 连接建立后立即发送在线用户数
        try {
            emitter.send(SseEmitter.event()
                    .name(SseTopics.ONLINE_COUNT)
                    .data(sessionRegistry.getOnlineUserCount()));
        } catch (IOException e) {
            log.warn("发送初始在线用户数失败: {}", e.getMessage());
        }

        log.info("用户[{}]SSE连接已建立，当前在线用户数: {}", username, sessionRegistry.getOnlineUserCount());

        // 发送在线用户数变更
        sendOnlineCount();

        return emitter;
    }

    /**
     * 发送字典变更事件
     *
     * @param dictCode 字典编码
     */
    public void sendDictChange(String dictCode) {
        if (dictCode == null || dictCode.isEmpty()) {
            log.warn("字典编码为空，跳过发送");
            return;
        }

        DictChangeEvent event = new DictChangeEvent(dictCode);
        sessionRegistry.broadcast(SseTopics.DICT, event);
        log.info("已发送字典变更通知: dictCode={}", dictCode);
    }

    /**
     * 发送在线用户数
     */
    public void sendOnlineCount() {
        int count = sessionRegistry.getOnlineUserCount();
        sessionRegistry.broadcast(SseTopics.ONLINE_COUNT, count);
        log.debug("已发送在线用户数: {}", count);
    }

    /**
     * 发送消息给指定用户
     *
     * @param username 用户名
     * @param eventName 事件名称
     * @param data 数据
     */
    public void sendToUser(String username, String eventName, Object data) {
        if (username == null || username.isEmpty()) {
            log.warn("用户名为空，无法发送消息");
            return;
        }
        sessionRegistry.sendToUser(username, eventName, data);
        log.info("已向用户[{}]发送事件[{}]", username, eventName);
    }

    /**
     * 获取在线用户列表
     *
     * @return 在线用户列表
     */
    public List<OnlineUserDTO> getOnlineUsers() {
        return sessionRegistry.getOnlineUsers();
    }

    /**
     * 获取在线用户数
     *
     * @return 在线用户数
     */
    public int getOnlineUserCount() {
        return sessionRegistry.getOnlineUserCount();
    }

    /**
     * 发送系统消息
     *
     * @param message 消息内容
     */
    public void sendSystemMessage(String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        Map<String, Object> systemMessage = Map.of(
                "sender", "系统通知",
                "content", message,
                "timestamp", System.currentTimeMillis()
        );
        sessionRegistry.broadcast(SseTopics.SYSTEM, systemMessage);
        log.info("已发送系统消息: {}", message);
    }
}
