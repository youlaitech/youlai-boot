package com.youlai.boot.platform.websocket.session;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class UserSessionRegistry {

    private final Map<String, Set<String>> userSessionsMap = new ConcurrentHashMap<>();
    private final Map<String, SessionInfo> sessionDetailsMap = new ConcurrentHashMap<>();

    public void userConnected(String username, String sessionId) {
        userSessionsMap.computeIfAbsent(username, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        sessionDetailsMap.put(sessionId, new SessionInfo(username, sessionId, System.currentTimeMillis()));
    }

    public void userDisconnected(String username) {
        Set<String> sessions = userSessionsMap.remove(username);
        if (sessions == null) {
            return;
        }
        sessions.forEach(sessionDetailsMap::remove);
    }

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
            userSessionsMap.remove(username);
        }
    }

    public int getOnlineUserCount() {
        return userSessionsMap.size();
    }

    public int getUserSessionCount(String username) {
        Set<String> sessions = userSessionsMap.get(username);
        return sessions != null ? sessions.size() : 0;
    }

    public int getTotalSessionCount() {
        return sessionDetailsMap.size();
    }

    public boolean isUserOnline(String username) {
        Set<String> sessions = userSessionsMap.get(username);
        return sessions != null && !sessions.isEmpty();
    }

    public List<OnlineUserDto> getOnlineUsers() {
        return userSessionsMap.entrySet().stream()
                .map(entry -> {
                    String username = entry.getKey();
                    Set<String> sessions = entry.getValue();
                    long earliestLoginTime = sessions.stream()
                            .map(sessionDetailsMap::get)
                            .filter(info -> info != null)
                            .mapToLong(SessionInfo::getConnectTime)
                            .min()
                            .orElse(System.currentTimeMillis());

                    return new OnlineUserDto(username, sessions.size(), earliestLoginTime);
                })
                .collect(Collectors.toList());
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class SessionInfo {
        private String username;
        private String sessionId;
        private long connectTime;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OnlineUserDto {
        private String username;
        private int sessionCount;
        private long loginTime;
    }
}
