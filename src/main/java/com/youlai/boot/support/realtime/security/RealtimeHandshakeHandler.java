package com.youlai.boot.support.realtime.security;

import com.youlai.boot.support.realtime.RealtimeSessionManager;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import java.security.Principal;
import java.util.Map;

/**
 * Principal 仅公开用户 ID；认证材料在建立连接后移入私有状态
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
@Component
public class RealtimeHandshakeHandler extends DefaultHandshakeHandler {
    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler handler,
                                      Map<String, Object> attributes) {
        var ticket = (RealtimeTicketService.VerifiedTicket) attributes.get(RealtimeHandshakeInterceptor.TICKET);
        return () -> ticket.grant().userId().toString();
    }

    /**
     * 执行协议升级，失败时释放连接配额
     */
    public boolean handshake(ServerHttpRequest request, ServerHttpResponse response,
                             WebSocketHandler handler, Map<String, Object> attributes) {
        boolean upgraded = false;
        try {
            upgraded = doHandshake(request, response, handler, attributes);
            return upgraded;
        } finally {
            if (!upgraded) {
                var reservation = (RealtimeSessionManager.Reservation) attributes.get(RealtimeHandshakeInterceptor.RESERVATION);
                if (reservation != null) {
                    reservation.close();
                }
            }
        }
    }
}
