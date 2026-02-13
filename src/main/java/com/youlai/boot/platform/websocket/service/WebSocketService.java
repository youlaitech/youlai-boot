package com.youlai.boot.platform.websocket.service;

import com.youlai.boot.platform.websocket.dto.OnlineUserDTO;

import java.util.List;

/**
 * WebSocket服务接口
 * <p>
 * 提供与WebSocket连接管理相关的功能，包括：
 * - 用户连接/断开事件处理
 * - 字典数据变更通知
 * - 系统消息推送
 * </p>
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
public interface WebSocketService {

    /**
     * 处理用户连接事件
     *
     * @param username  用户名
     * @param sessionId WebSocket会话ID
     */
    void userConnected(String username, String sessionId);

    /**
     * 处理用户断开连接事件
     *
     * @param username 用户名
     */
    void userDisconnected(String username);

    /**
     * 广播字典数据变更通知
     *
     * @param dictCode 字典编码
     */
    void broadcastDictChange(String dictCode);

    /**
     * 发送系统通知给特定用户
     *
     * @param username 目标用户名
     * @param message  通知消息内容
     */
    void sendNotification(String username, Object message);

    /**
     * 获取在线用户列表
     *
     * @return 在线用户信息列表
     */
    List<OnlineUserDTO> getOnlineUsers();
}
