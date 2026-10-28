package com.youlai.boot.support.realtime;

import com.youlai.boot.support.realtime.protocol.RealtimeMessageCodec;
import com.youlai.boot.support.realtime.security.RealtimeHandshakeInterceptor;
import com.youlai.boot.support.realtime.security.RealtimeTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.adapter.NativeWebSocketSession;

/**
 * 接收 WebSocket 回调并转交连接管理器
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
@Component
@RequiredArgsConstructor
public class RealtimeWebSocketHandler extends TextWebSocketHandler {
    private final RealtimeSessionManager sessions;
    private final RealtimeMessageCodec codec;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // Tomcat 的同步发送有独立阻塞上限，不依赖 Future 超时
        if (session instanceof NativeWebSocketSession nativeSession) {
            jakarta.websocket.Session delegate = nativeSession.getNativeSession(jakarta.websocket.Session.class);
            if (delegate != null) {
                delegate.getUserProperties().put("org.apache.tomcat.websocket.BLOCKING_SEND_TIMEOUT", 5_000L);
            }
        }
        var ticket = (RealtimeTicketService.VerifiedTicket) session.getAttributes()
                .remove(RealtimeHandshakeInterceptor.TICKET);
        var reservation = (RealtimeSessionManager.Reservation) session.getAttributes()
                .remove(RealtimeHandshakeInterceptor.RESERVATION);
        sessions.register(session, ticket, reservation);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            sessions.handlePong(session, codec.decodePong(message.getPayload()));
        } catch (RuntimeException exception) {
            sessions.requestClose(session, 1008, "invalid protocol");
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        sessions.requestClose(session, 1008, "text messages required");
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        sessions.requestClose(session, 1011, "transport error");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.terminated(session);
    }
}
