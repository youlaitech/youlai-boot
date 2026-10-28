package com.youlai.boot.support.realtime.security;

import com.youlai.boot.support.realtime.RealtimeSessionManager;
import com.youlai.boot.support.realtime.config.RealtimeProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.WebUtils;
import java.util.Map;

/**
 * 消费票据后将可信身份和配额转交给连接，不在 URL 中传访问令牌
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
@Component
@RequiredArgsConstructor
public class RealtimeHandshakeInterceptor implements HandshakeInterceptor {
    public static final String TICKET = RealtimeHandshakeInterceptor.class.getName() + ".ticket";
    public static final String RESERVATION = RealtimeHandshakeInterceptor.class.getName() + ".reservation";
    private final RealtimeTicketService tickets;
    private final RealtimeSessionManager sessions;
    private final RealtimeProperties properties;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler handler, Map<String, Object> attributes) {
        String origin = request.getHeaders().getOrigin();
        if (origin != null && !WebUtils.isSameOrigin(request)
                && !properties.getAllowedOrigins().contains(origin)) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }
        var reservation = sessions.reserve();
        if (reservation == null) {
            response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            return false;
        }
        try {
            var params = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();
            if (params.size() != 1 || params.get("ticket") == null || params.get("ticket").size() != 1) {
                throw new BadCredentialsException("Invalid ticket query");
            }
            var ticket = tickets.consume(params.getFirst("ticket"));
            if (origin == null && "web".equals(ticket.grant().platform())) {
                response.setStatusCode(HttpStatus.FORBIDDEN);
                reservation.close();
                return false;
            }
            attributes.put(TICKET, ticket);
            attributes.put(RESERVATION, reservation);
            if (request instanceof ServletServerHttpRequest servlet) {
                servlet.getServletRequest().setAttribute(RESERVATION, reservation);
            }
            return true;
        } catch (AccessDeniedException exception) {
            reservation.close();
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        } catch (BadCredentialsException exception) {
            reservation.close();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        } catch (Exception exception) {
            reservation.close();
            response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler handler, Exception exception) {
        if (request instanceof ServletServerHttpRequest servlet
                && response instanceof ServletServerHttpResponse servletResponse
                && servletResponse.getServletResponse().getStatus() != 101) {
            Object reserved = servlet.getServletRequest().getAttribute(RESERVATION);
            if (reserved instanceof RealtimeSessionManager.Reservation reservation) {
                reservation.close();
            }
        }
    }
}
