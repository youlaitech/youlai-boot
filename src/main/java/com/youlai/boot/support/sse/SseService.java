package com.youlai.boot.support.sse;

import com.youlai.boot.support.sse.constant.SseEventConstants;
import com.youlai.boot.support.sse.constant.SseTopics;
import com.youlai.boot.support.sse.model.OnlineUserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * SSE 推送服务
 * <p>
 * 组合 {@link SseSessionManager} 提供 SSE 建连、广播、定向推送、在线用户统计等能力。
 *
 * @author Ray.Hao
 * @since 3.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SseService {

    private final SseSessionManager sessionManager;

    /**
     * 创建 SSE 连接
     *
     * @param username 用户名
     * @return SseEmitter
     */
    public SseEmitter createConnection(String username) {
        SseEmitter emitter = new SseEmitter(SseSessionManager.TIMEOUT);
        if (username == null || username.isEmpty()) {
            log.warn("创建SSE连接失败：用户名为空");
            emitter.completeWithError(new IllegalArgumentException("用户名为空"));
            return emitter;
        }

        sessionManager.userConnected(username, emitter);

        // 连接建立后立即发送在线用户数
        try {
            emitter.send(SseEmitter.event()
                    .name(SseEventConstants.ONLINE_USERS)
                    .data(sessionManager.getOnlineUserCount()));
        } catch (IOException e) {
            log.warn("发送初始在线用户数失败: {}", e.getMessage());
        }

        log.info("用户[{}]SSE连接已建立，当前在线用户数: {}", username, sessionManager.getOnlineUserCount());
        sendOnlineCount();

        return emitter;
    }

    /**
     * 广播事件到所有在线连接
     *
     * @param topic 事件主题
     * @param data  数据
     */
    public void broadcast(String topic, Object data) {
        sessionManager.broadcast(topic, data);
    }

    /**
     * 向指定用户发送事件
     *
     * @param username 用户名
     * @param topic    事件主题
     * @param data     数据
     */
    public void sendToUser(String username, String topic, Object data) {
        sessionManager.sendToUser(username, topic, data);
    }

    /**
     * 广播在线用户数
     */
    public void sendOnlineCount() {
        int count = sessionManager.getOnlineUserCount();
        sessionManager.broadcast(SseEventConstants.ONLINE_USERS, count);
        log.debug("已发送在线用户数: {}", count);
    }

    /**
     * 发送系统消息（全员广播）
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
        sessionManager.broadcast(SseTopics.SYSTEM, systemMessage);
        log.info("已发送系统消息: {}", message);
    }

    /**
     * 获取在线用户列表
     *
     * @return 在线用户列表
     */
    public List<OnlineUserDTO> getOnlineUsers() {
        return sessionManager.getOnlineUsernames().stream()
                .map(username -> {
                    int sessionCount = sessionManager.getUserConnectionCount(username);
                    long loginTime = sessionManager.getEarliestLoginTime(username);
                    return new OnlineUserDTO(username, sessionCount, loginTime);
                })
                .toList();
    }

    /**
     * 获取在线用户数
     *
     * @return 在线用户数
     */
    public int getOnlineUserCount() {
        return sessionManager.getOnlineUserCount();
    }
}
