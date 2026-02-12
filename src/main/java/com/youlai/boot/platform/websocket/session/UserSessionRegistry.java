package com.youlai.boot.platform.websocket.session;

import com.youlai.boot.platform.websocket.dto.OnlineUserDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * WebSocket 用户会话注册表
 * <p>
 * 维护WebSocket连接的用户会话信息，支持多设备同时登录。
 * 采用双Map结构实现高效查询：
 * <ul>
 *   <li>userSessionsMap: 用户名 -> 会话ID集合（支持多设备）</li>
 *   <li>sessionDetailsMap: 会话ID -> 会话详情（快速定位用户）</li>
 * </ul>
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
@Slf4j
@Component
public class UserSessionRegistry {

    /**
     * 用户会话映射表
     * <p>
     * Key: 用户名
     * Value: 该用户所有WebSocket会话ID集合（支持多设备登录）
     */
    private final Map<String, Set<String>> userSessionsMap = new ConcurrentHashMap<>();

    /**
     * 会话详情映射表
     * <p>
     * Key: WebSocket会话ID
     * Value: 会话详情（包含用户名、连接时间等）
     */
    private final Map<String, SessionInfo> sessionDetailsMap = new ConcurrentHashMap<>();

    /**
     * 用户上线（建立WebSocket连接）
     *
     * @param username  用户名
     * @param sessionId WebSocket会话ID
     */
    public void userConnected(String username, String sessionId) {
        userSessionsMap.computeIfAbsent(username, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        sessionDetailsMap.put(sessionId, new SessionInfo(username, sessionId, System.currentTimeMillis()));
        log.debug("用户[{}]会话[{}]已注册", username, sessionId);
    }

    /**
     * 用户下线（断开所有WebSocket连接）
     * <p>
     * 移除该用户的所有会话信息
     *
     * @param username 用户名
     */
    public void userDisconnected(String username) {
        Set<String> sessions = userSessionsMap.remove(username);
        if (sessions == null) {
            return;
        }
        sessions.forEach(sessionDetailsMap::remove);
        log.debug("用户[{}]已下线，移除{}个会话", username, sessions.size());
    }

    /**
     * 移除指定会话（单设备下线）
     * <p>
     * 当用户某一设备断开连接时调用，保留其他设备的会话
     *
     * @param sessionId WebSocket会话ID
     */
    public void removeSession(String sessionId) {
        SessionInfo sessionInfo = sessionDetailsMap.remove(sessionId);
        if (sessionInfo == null) {
            return;
        }

        String username = sessionInfo.getUsername();
        Set<String> sessions = userSessionsMap.get(username);
        if (sessions == null) {
            return;
        }

        sessions.remove(sessionId);
        if (sessions.isEmpty()) {
            // 该用户没有任何会话了，移除用户记录
            userSessionsMap.remove(username);
            log.debug("用户[{}]最后一个会话已移除", username);
        }
    }

    /**
     * 获取在线用户数量
     *
     * @return 当前在线用户数（非会话数）
     */
    public int getOnlineUserCount() {
        return userSessionsMap.size();
    }

    /**
     * 获取指定用户的会话数量
     *
     * @param username 用户名
     * @return 该用户的WebSocket会话数量（多设备登录时大于1）
     */
    public int getUserSessionCount(String username) {
        Set<String> sessions = userSessionsMap.get(username);
        return sessions != null ? sessions.size() : 0;
    }

    /**
     * 获取在线会话总数
     *
     * @return 所有WebSocket会话的总数（包含多设备）
     */
    public int getTotalSessionCount() {
        return sessionDetailsMap.size();
    }

    /**
     * 检查用户是否在线
     *
     * @param username 用户名
     * @return 是否在线（至少有一个活跃会话）
     */
    public boolean isUserOnline(String username) {
        Set<String> sessions = userSessionsMap.get(username);
        return sessions != null && !sessions.isEmpty();
    }

    /**
     * 获取所有在线用户列表
     *
     * @return 在线用户信息列表
     */
    public List<OnlineUserDTO> getOnlineUsers() {
        return userSessionsMap.entrySet().stream()
                .map(entry -> {
                    String username = entry.getKey();
                    Set<String> sessions = entry.getValue();
                    // 取最早的连接时间作为登录时间
                    long earliestLoginTime = sessions.stream()
                            .map(sessionDetailsMap::get)
                            .filter(info -> info != null)
                            .mapToLong(SessionInfo::getConnectTime)
                            .min()
                            .orElse(System.currentTimeMillis());

                    return new OnlineUserDTO(username, sessions.size(), earliestLoginTime);
                })
                .collect(Collectors.toList());
    }

    /**
     * WebSocket 会话详情（内部使用）
     */
    @Data
    @AllArgsConstructor
    private static class SessionInfo {
        /** 用户名 */
        private String username;
        /** WebSocket会话ID */
        private String sessionId;
        /** 连接时间戳 */
        private long connectTime;
    }
}
