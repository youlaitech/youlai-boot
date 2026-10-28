package com.youlai.boot.support.realtime.model;

/**
 * WebSocket 握手的一次性票据
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
public record RealtimeTicketResponse(String ticket) {
    @Override
    public String toString() {
        return "RealtimeTicketResponse[REDACTED]";
    }
}
