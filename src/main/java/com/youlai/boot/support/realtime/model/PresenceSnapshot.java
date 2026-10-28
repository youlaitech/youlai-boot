package com.youlai.boot.support.realtime.model;

/**
 * 指定节点在采样时刻的在线统计
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
public record PresenceSnapshot(int onlineUsers, int onlineConnections, String nodeId, String observedAt) {
}
